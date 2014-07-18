var seedData = {
    id: '12345',
    clientId: uuid(),
    content: "I'm a Jedi"
    // content: {
    //     id: "123456-654321",
    //     name: "Luke Skywalker",
    //     profession: "Jedi",
    //     hobbies: [
    //         {
    //             id: uuid(),
    //             description: "Fighting the Dark Side"
    //         },
    //         {
    //             id: uuid(),
    //             description: "going into Tosche Station to pick up some power converters"
    //         },
    //         {
    //             id: uuid(),
    //             description: "Kissing his sister"
    //         },
    //         {
    //             id: uuid(),
    //             description: "Bulls eyeing Womprats on his T-16"
    //         }
    //     ]
    // }
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
        //app.list.append( _.template( listTemplate, doc.content ) );
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
            app.currentData.content = val;
            // for( var key in app.currentData.content ) {
            //     if( key === id ) {
            //         app.currentData.content[ key ] = val;
            //         break;
            //     }
            // }
        }

        var edits = app.syncEngine.diff(app.currentData);
        app.syncClient.sendEdit(edits);
        //app.syncClient.addDoc(app.currentData);

        // app.ag.syncer.save( app.currentData, {
        //     success: function( document ) {
        //         console.log( "success", document );
        //         app.updateUI( document );
        //         $('.results').hide();
        //     },
        //     conflict: function( error, newModel, change ) {
        //         console.log( "Conflict Saving", error, newModel, change );
        //         app.newModel = newModel;
        //         app.change = change;
        //         app.addError.apply( this, arguments );
        //     },
        //     error: function( error ) {
        //         console.log( "Error Saving", error );
        //     }
        // });
        app.updateUI( app.currentData );
    },
    onmessage: function ( e ) {
        var data = JSON.parse(e.data),
            doc;

        switch(data.result) {
        case 'ADDED':
            console.log('ADDED');
            break;
        case 'PATCHED':
            console.log('PATCHED');
            break;
        default:
            console.log(data);
            break;
        }

        if(!data.result) {
            app.syncEngine.patch(data);
        }

        doc = app.syncEngine.getDocument("12345");
        app.updateUI(doc);
    },
    onopen: function ( e ) {
        console.log( e );
        app.syncEngine.addDocument(seedData);
        app.syncClient.addDoc(seedData);
    },
    init: function () {

        console.log( 'initing' );

        this.syncEngine = new Sync.Engine();

        this.syncClient = new Sync.Client({
            serverUrl: 'ws://localhost:7777/sync',
            onmessage: this.onmessage,
            onopen: this.onopen
        });

        this.ENTER_KEY = 13;

        this.content = $( "div .detail" );
        this.list = $( "#hobby-list" );

        this.content.on( "dblclick", "label", this.edit );
        this.content.on( "keypress", '.edit', this.blurOnEnter );
        this.content.on( "blur", ".edit", this.update );
        this.list.on( "dblclick", "label", this.edit );
        this.list.on( "keypress", '.edit', this.blurOnEnter );
        this.list.on( "blur", ".edit", this.update );

    }
};

function uuid () {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function( c ) {
        var r = Math.random()*16|0, v = c === 'x' ? r : (r&0x3|0x8);
        return v.toString( 16 );
    });
}


app.init();
