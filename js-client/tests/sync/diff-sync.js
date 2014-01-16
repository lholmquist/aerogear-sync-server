(function() {

    module('Sync integration test');

    test('add document', function() {
        var documentId = uuid();
        var ws = new WebSocket("ws://localhost:7777/sync");
        var addMsg = JSON.stringify( { msgType: 'add', docId: documentId, content: 'Do or do not, there is no try.' } );
        var result;
        console.log( addMsg );

        ws.onopen = function ( evt ) {
            ws.send(addMsg);
        };

        ws.onmessage = function ( evt ) {
            var json = JSON.parse( evt.data );
            result = json.result;
        };
        // how do I make the above synchronous...wait for the ws.onopen callback
        equal( result, 'CREATED', 'Document should have been added' );
    });

    function uuid()
    {
        return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
            var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
            return v.toString( 16 );
        });
    }

})();
