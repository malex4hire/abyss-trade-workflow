// Fetch streaming preserves bearer authentication without putting tokens in URLs.
export async function subscribeDeclarations(
  token: string,
  signal: AbortSignal,
  onChange: () => void,
  onError: (message: string | null) => void,
) {
  let delay = 1000;
  while (!signal.aborted) {
    try {
      const response = await fetch('/api/events', {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        signal,
      });
      if (!response.ok) {
        onError(`Subscription failed (${response.status})`);
        if (response.status === 401 || response.status === 403) return;
        throw new Error('Subscription unavailable');
      }
      if (!response.body) throw new Error('Streaming unavailable');
      onError(null);
      delay = 1000;
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      try {
        while (!signal.aborted) {
          const { value, done } = await reader.read();
          if (done) break;
          buffer += decoder.decode(value, { stream: true });
          let boundary;
          while ((boundary = buffer.indexOf('\n\n')) >= 0) {
            const frame = buffer.slice(0, boundary);
            buffer = buffer.slice(boundary + 2);
            if (/^event:(?: ?)(changed|reset)$/m.test(frame)) onChange();
          }
        }
      } finally {
        await reader.cancel().catch(() => undefined);
        reader.releaseLock();
      }
      if (!signal.aborted) onError('Subscription disconnected; reconnecting');
    } catch (error) {
      if (signal.aborted) return;
      onError(error instanceof Error ? error.message : 'Subscription disconnected');
    }
    if (signal.aborted) return;
    await new Promise<void>((resolve) => {
      const finish = () => {
        clearTimeout(timer);
        signal.removeEventListener('abort', finish);
        resolve();
      };
      const timer = setTimeout(finish, delay);
      signal.addEventListener('abort', finish, { once: true });
    });
    delay = Math.min(delay * 2, 30000);
  }
}
