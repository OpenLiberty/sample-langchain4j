var input = document.getElementById('myMessage');
var sendButton = document.getElementById('sendButton');
var messages = document.getElementById('messagesTableBody');

function sendMessage() {
    var message = input.value;
    appendMessage('my-msg', message, new Date().toLocaleTimeString());
    appendMessage('thinking-msg', 'thinking...');
    webSocket.send(message);
    input.value = '';
    sendButton.disabled = true;
};

function appendMessage(className, textContent, timeContent) {
    var messageRow = document.createElement('tr');
    messageRow.innerHTML = '<td><p></p></td><td></td>';
    messageRow.querySelector('p').className = className;
    messageRow.querySelector('p').textContent = textContent;
    messageRow.querySelector('td:nth-child(2)').textContent = timeContent;
    messages.append(messageRow);
}

// Connect to websocket
var webSocket = new WebSocket('/streamingchat');

webSocket.onopen = function (event) {
    console.log(event);
};

webSocket.onmessage = function (event) {
    if (event.data != '') {
        if (messages.lastChild.querySelector('.thinking-msg')) {  // if this is the first token
            messages.removeChild(messages.lastChild);
            appendMessage('agent-msg', '', new Date().toLocaleTimeString());
        }
        messages.lastChild.querySelector('.agent-msg').textContent += event.data;
    } else {  // stream ends with empty string
        sendButton.disabled = false;
    }
};

webSocket.onerror = function (event) {
    console.log('Error: ' + event);
};

webSocket.onclose = function (event) {
    console.log(event);
    appendMessage('agent-msg', 'The connection to the streaming chat serivce was closed.');
    sendButton.disabled = true;
};
