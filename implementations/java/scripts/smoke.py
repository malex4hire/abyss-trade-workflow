#!/usr/bin/env python3
"""Verify the real API/PostgreSQL/Kafka stack started by Docker Compose."""

import concurrent.futures
import json
import os
import time
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
API_URL = os.environ.get("API_URL", "http://localhost:18080")


def demo_token():
    if token := os.environ.get("DEMO_TOKEN"):
        return token
    for line in (ROOT / ".env").read_text().splitlines():
        if line.startswith("DEMO_TOKEN="):
            return line.split("=", 1)[1]
    raise RuntimeError("Set DEMO_TOKEN or generate .env with ./demo")


TOKEN = demo_token()


def request(path, method="GET", data=None, token=TOKEN):
    payload = None if data is None else json.dumps(data).encode()
    req = urllib.request.Request(
        API_URL + path,
        method=method,
        headers={
            "Authorization": "Bearer " + token,
            "Content-Type": "application/json",
        },
        data=payload,
    )
    try:
        with urllib.request.urlopen(req, timeout=10) as response:
            return response.status, json.load(response)
    except urllib.error.HTTPError as error:
        return error.code, json.load(error)


def await_result(declaration_id):
    for _ in range(60):
        status, body = request("/api/declarations/" + declaration_id)
        if status == 200 and body["status"] in ("accepted", "rejected"):
            return body
        time.sleep(1)
    raise AssertionError("Worker did not finish within 60 seconds")


def main():
    assert request("/api/declarations", token="wrong")[0] == 401
    data = {
        "importer": "Synthetic Importer",
        "originCountry": "CA",
        "commodityCode": "847130",
        "declaredValueCents": 125000,
        "currency": "USD",
        "shipmentReference": "SMOKE",
    }
    assert request("/api/declarations", "POST", {**data, "declaredValueCents": -1})[0] == 400
    assert request("/api/declarations/not-a-uuid")[0] == 404

    for amount, expected in [(125000, "accepted"), (10000001, "rejected")]:
        status, created = request(
            "/api/declarations", "POST", {**data, "declaredValueCents": amount}
        )
        assert status == 201, (status, created)
        declaration_id = created["id"]
        submit_path = "/api/declarations/" + declaration_id + "/submit"
        with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
            replies = list(pool.map(lambda _: request(submit_path, "POST"), range(8)))
        assert all(code in (200, 202) for code, _ in replies), replies
        assert sum(code == 202 for code, _ in replies) == 1, replies

        final = await_result(declaration_id)
        assert final["status"] == expected, final
        actions = [entry["action"] for entry in final["audit"]]
        assert actions == ["draft_created", "submitted", expected], actions
        assert request(submit_path, "POST")[0] == 200
        assert request("/api/declarations/" + declaration_id)[1]["audit"] == final["audit"]
        print("PASS:", expected, "one transition for concurrent submits of ID", declaration_id)

    print("PASS: authentication, validation, missing record, decisions, submit retries, audit")


if __name__ == "__main__":
    main()
