var http = require('http'),
    express = require('express'),
    app = express();


app.use(express.static(__dirname + '/app'));
app.use('/src', express.static('./src'));
app.use('/lib', express.static('./lib'));
app.use('/bower_components', express.static('./bower_components'));

var server = http.createServer(app);
server.listen(8080);
