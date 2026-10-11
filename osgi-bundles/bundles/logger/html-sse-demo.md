### HTML SSE Demo

1. Build the project: `mvn clean install -pl osgi-bundles/bundles/logger`
2. Copy the JAR to `platform` plugin directory
3. Copy HTML content below to `<any-directory>`
4. Run this using `python3 -m http.server 3000 -d <any-directory>` or `npx serve -l 3000 <any-directory>`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>SSE Logger Client</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body { font-family: monospace; padding: 20px; background: #1e1e1e; color: #d4d4d4; }
        h1 { font-size: 1.2em; margin-bottom: 16px; color: #569cd6; }
        .controls { display: flex; gap: 10px; flex-wrap: wrap; align-items: flex-end; margin-bottom: 16px; }
        .field { display: flex; flex-direction: column; gap: 4px; }
        .field label { font-size: 0.8em; color: #9cdcfe; }
        .field input { background: #2d2d2d; border: 1px solid #3c3c3c; color: #d4d4d4; padding: 6px 10px; font-family: monospace; font-size: 0.9em; width: 260px; }
        .field input:focus { outline: none; border-color: #569cd6; }
        button { padding: 6px 16px; font-family: monospace; font-size: 0.9em; cursor: pointer; border: 1px solid #3c3c3c; }
        #connectBtn { background: #2ea043; color: #fff; border-color: #2ea043; }
        #connectBtn.connected { background: #d73a49; border-color: #d73a49; }
        #clearBtn { background: #2d2d2d; color: #d4d4d4; }
        .status { font-size: 0.8em; padding: 4px 8px; border-radius: 3px; align-self: center; }
        .status.open { color: #2ea043; }
        .status.closed { color: #d73a49; }
        #output { width: 100%; height: calc(100vh - 160px); background: #1a1a1a; border: 1px solid #3c3c3c; padding: 10px; overflow-y: auto; white-space: pre-wrap; word-break: break-all; font-size: 0.85em; line-height: 1.5; }
        .msg-data { color: #ce9178; }
        .msg-heartbeat { color: #6a9955; }
        .msg-event { color: #569cd6; }
        .msg-id { color: #b5cea8; }
        .msg-system { color: #dcdcaa; font-style: italic; }
    </style>
</head>
<body>
    <h1>SSE Logger Client</h1>
    <div class="controls">
        <div class="field">
            <label>Base URL</label>
            <input id="baseUrl" value="http://localhost:8080/plugins/killbill-osgi-logger/" />
        </div>
        <div class="field">
            <label>accountId (UUID)</label>
            <input id="accountId" placeholder="optional" />
        </div>
        <div class="field">
            <label>userToken</label>
            <input id="userToken" placeholder="optional" />
        </div>
        <button id="connectBtn">Connect</button>
        <button id="clearBtn">Clear</button>
        <span id="status" class="status closed">● Disconnected</span>
    </div>
    <div id="output"></div>

    <script type="text/javascript">
        // -- SSE Client --
        let eventSource = null;

        function buildUrl() {
            const base = document.getElementById('baseUrl').value.replace(/\/$/, '');
            const accountId = document.getElementById('accountId').value.trim();
            const userToken = document.getElementById('userToken').value.trim();

            const params = [];
            if (accountId) params.push('accountId=' + encodeURIComponent(accountId));
            if (userToken) params.push('userToken=' + encodeURIComponent(userToken));

            return params.length > 0 ? base + '/?' + params.join('&') : base + '/';
        }

        function appendMessage(text, className) {
            const output = document.getElementById('output');
            const line = document.createElement('div');
            line.className = className;
            line.textContent = '[' + new Date().toISOString().substring(11, 23) + '] ' + text;
            output.appendChild(line);
            output.scrollTop = output.scrollHeight;
        }

        function setStatus(connected) {
            const status = document.getElementById('status');
            const btn = document.getElementById('connectBtn');
            if (connected) {
                status.textContent = '● Connected';
                status.className = 'status open';
                btn.textContent = 'Disconnect';
                btn.classList.add('connected');
            } else {
                status.textContent = '● Disconnected';
                status.className = 'status closed';
                btn.textContent = 'Connect';
                btn.classList.remove('connected');
            }
        }

        function connect() {
            const url = buildUrl();
            appendMessage('Connecting to: ' + url, 'msg-system');

            eventSource = new EventSource(url);

            eventSource.onopen = function() {
                setStatus(true);
                appendMessage('Connection opened', 'msg-system');
            };

            // Default message event (unnamed events / data-only)
            eventSource.onmessage = function(event) {
                let display = 'data: ' + event.data;
                if (event.lastEventId) display += '  [id: ' + event.lastEventId + ']';
                appendMessage(display, 'msg-data');
            };

            // Named "heartbeat" event
            eventSource.addEventListener('heartbeat', function(event) {
                let display = 'event: heartbeat';
                if (event.lastEventId) display += '  [id: ' + event.lastEventId + ']';
                appendMessage(display, 'msg-heartbeat');
            });

            // Named "message" event (some SSE impls use explicit event:message)
            eventSource.addEventListener('message', function(event) {
                let display = 'data: ' + event.data;
                if (event.lastEventId) display += '  [id: ' + event.lastEventId + ']';
                appendMessage(display, 'msg-data');
            });

            eventSource.onerror = function() {
                if (eventSource.readyState === EventSource.CLOSED) {
                    appendMessage('Connection closed by server', 'msg-system');
                    setStatus(false);
                    eventSource = null;
                } else {
                    appendMessage('Connection error — reconnecting...', 'msg-system');
                }
            };
        }

        function disconnect() {
            if (eventSource) {
                eventSource.close();
                eventSource = null;
                setStatus(false);
                appendMessage('Disconnected by user', 'msg-system');
            }
        }

        // -- Event Handlers --
        document.getElementById('connectBtn').addEventListener('click', function() {
            if (eventSource) {
                disconnect();
            } else {
                connect();
            }
        });

        document.getElementById('clearBtn').addEventListener('click', function() {
            document.getElementById('output').innerHTML = '';
        });
    </script>
</body>
</html>

```