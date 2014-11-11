var seedData = {
    id: '12345',
    clientId: uuid(),
    content: {
        name: "Luke Skywalker",
        profession: "Jedi",
        hobbies: [
            {
                id: uuid(),
                description: "Fighting the Dark Side"
            },
            {
                id: uuid(),
                description: "going into Tosche Station to pick up some power converters"
            },
            {
                id: uuid(),
                description: "Kissing his sister"
            },
            {
                id: uuid(),
                description: "Bulls eyeing Womprats on his T-16"
            }
        ]
    }
};

var app = {
    currentData: {},
    updateUI: function( doc ) {
        doc = app.currentData = doc ? doc : app.currentData || {};
        var detailTemplate = $( "#detail" ).html(),
            listTemplate = $( "#hobbies" ).html();

        app.content.empty();
        app.list.empty();
        app.content.append( _.template( detailTemplate, doc ) );
        app.list.append( _.template( listTemplate, doc.content ) );
    },
    edit: function () {
        var $input = $( this ).closest( "li" ).addClass( "editing" ).find( ".edit" );
        var val = $input.val();

        $input.val( val ).focus();
    },
    blurOnEnter: function( e ) {
        if (e.which === app.ENTER_KEY) {
            e.target.blur();
        }
    },
    update: function() {
        var val = $.trim($(this).removeClass('editing').val()),
            id = this.id;

        if( $(this).hasClass("hobby") ) {
            app.currentData.content.hobbies = app.currentData.content.hobbies.map( function( item ) {
                if( item.id === id ) {
                    item.description = val;
                    return item;
                }
                return item;
            });
        } else {
            app.currentData.content.profession = val;
        }
        console.log('app.currentData:', app.currentData);

        var edits = app.syncClient.diff(app.currentData);
        app.syncClient.sendEdits(edits);
        app.updateUI( app.currentData );
    },
    onmessage: function ( e ) {
        var data = JSON.parse(e.data),
            doc;
        console.log('data:', data);

        if( data ) {
            app.syncClient.patch( data );
        }

        doc = app.syncClient.getDocument("12345");
        app.updateUI(doc);
    },
    onopen: function ( e ) {
        if ( app.initializing === true ) {
            app.syncClient.addDocument(seedData);
            app.initializing = false;
        } else {
            app.syncClient.update( "12345" );
        }
    },
    onconnection: function( e ) {
        if ( $( "#connection" ).html() == 'Disconnect' ) {
            console.log('Going to disconnect...');
            app.syncClient.disconnect();
            $("#connection" ).html('Connect');
        } else {
            console.log('Going to connect...');
            app.syncClient.connect();
            $("#connection" ).html('Disconnect');
        }
        e.target.blur();
    },
    init: function () {

        console.log( 'initing' );

        this.initializing = true;
        this.syncClient = new AeroGear.SyncClient({
            serverUrl: 'ws://localhost:7777/sync',
            onmessage: this.onmessage,
            onopen: this.onopen,
        });

        this.ENTER_KEY = 13;

        this.content = $( "div .detail" );
        this.list = $( "#hobby-list" );
        this.connection = $( "#connection" );

        this.content.on( "dblclick", "label", this.edit );
        this.content.on( "keypress", '.edit', this.blurOnEnter );
        this.content.on( "blur", ".edit", this.update );
        this.list.on( "dblclick", "label", this.edit );
        this.list.on( "keypress", '.edit', this.blurOnEnter );
        this.list.on( "blur", ".edit", this.update );
        this.connection.click( this.onconnection );
    }
};

function uuid () {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
        var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
        return v.toString( 16 );
    });
}


app.init();
