FROM golang:1.24.9-alpine AS build
WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download
COPY . .
RUN go test ./cmd/... ./internal/... && CGO_ENABLED=0 go build -buildvcs=false -mod=readonly -o /trade ./cmd/trade

# Optional PostgreSQL transaction tests on the Compose network.
FROM build AS test
CMD ["go", "test", "-tags=integration", "-v", "./internal/store"]

FROM gcr.io/distroless/static-debian12:nonroot AS runtime
COPY --from=build /trade /trade
ENTRYPOINT ["/trade"]
