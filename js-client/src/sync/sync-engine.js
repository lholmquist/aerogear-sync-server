this.Sync = this.Sync || {};

Sync.Engine = function () {

    if ( ! ( this instanceof Sync.Engine ) ) {
        return new Sync.Engine();
    }

    var dm = AeroGear.DataManager( ['docs', 'shadows', 'backups', 'edits'] ), // Shouldn't depend on AG Datamaanger in the future
        dmp = new diff_match_patch();

    /**
     * Adds a new document to this sync engine.
     *
     * @param doc the document to add.
     */
    this.addDocument = function( doc ) {
        this._saveDocument( JSON.parse( JSON.stringify( doc ) ) );
        this._saveShadow( JSON.parse( JSON.stringify( doc ) ) );
        this._saveShadowBackup( JSON.parse( JSON.stringify( doc ) ) );
    };

    /**
     * Performs the client side of a differential sync.
     * When a client makes an update to it's document, it is first diffed against the shadow
     * document. The result of this is an {@link Edits} instance representing the changes.
     * There might be pending edits that represent edits that have not made it to the server
     * for some reason (for example packet drop). If a pending edit exits the contents (the diffs)
     * of the pending edit will be included in the returned Edits from this method.
     *
     * @param doc the updated document.
     * @returns {object} containing the diffs that between the clientDoc and it's shadow doc.
     */
    this.diff = function( doc ) {
        var diffDoc,
        shadow = dm.stores.shadows.read( doc.id )[0];

        diffDoc = {
            msgType: 'patch',
            id: doc.id,
            clientId: shadow.clientId,
            edits: [{
                clientVersion: shadow.clientVersion,
                serverVersion: shadow.serverVersion,
                // currently not implemented but we probably need this for checking the client and server shadow are identical be for patching.
                checksum: '',
                diffs: this._asAeroGearDiffs( dmp.diff_main( JSON.stringify( shadow.doc.content ), JSON.stringify( doc.content ) ) )
            }]
        };

        shadow.clientVersion++;
        shadow.doc.content = doc.content;
        this._saveShadow( shadow );

        return diffDoc;
    };

    this.processServerEdits = function( edits ) {
        var doc = this._saveDocument( this.patchDocument( edits ) ),
            backup = this.getBackup( doc.id );

        this._saveShadow( this.patchShadow( edits ) );
        backup.content = doc.content;
        backup.clientVersion++;
        this._saveBackup( backup );
        //remove pending edits.
    };

    this._asAeroGearDiffs = function( diffs ) {
        return diffs.map(function( value ) {
            return {operation: this._asAgOperation( value[0] ), text: value[1] };
        }.bind(this));
    };

    this._asDiffMatchPathDiffs = function( diffs ) {
        return diffs.map(function ( value ) {
            return [this._asDmpOperation ( value.operation ), value.text];
        }.bind(this));
    };

    this._asDmpOperation = function( op ) {
        if ( op === 'DELETE' ) {
            return -1;
        } else if ( op === 'ADD' ) {
            return 1;
        }
        return 0;
    };

    this._asAgOperation = function( op ) {
        if ( op === -1 ) {
            return 'DELETE';
        } else if ( op === 1 ) {
            return 'ADD';
        }
        return "UNCHANGED";
    };

    this.patchShadow = function( patchMsg ) {
        // First get the shadow document for this doc.id and clientId
        var i, patched, edit,
            shadow = this.getShadow( patchMsg.id ),
            edits = patchMsg.edits;
        //Iterate over the edits of the doc
        for ( i = 0; i < edits.length; i++ ) {
            edit = edits[i];

            //Check for dropped packets?
            // edit.clientVersion < shadow.ClientVersion
            if( edit.clientVersion < shadow.clientVersion ) {
                // Dropped packet?
                // restore from back
                // TODO
            }

            //check if we already have this one
            // IF SO discard the edit
            // edit.serverVersion < shadow.ServerVesion
            if( edit.serverVersion < shadow.serverVersion ) {
                // disCard Edit
                // TODO
            }

            //make sure the versions match
            if( edit.serverVersion === shadow.serverVersion && edit.clientVersion === shadow.clientVersion ) {
                // Good ,  Patch the shadow
                patched = this.applyEditsToShadow( edit, shadow );
                // increment the server version
                // save the shadow back
                try {
                    shadow.doc.content = JSON.parse( patched[ 0 ] );
                } catch( e ) {
                    shadow.doc.content = patched[ 0 ];
                }
                shadow.serverVersion++;

                this._saveShadow( shadow );
            }
        }

        console.log(patched);
        return shadow;
    };

    this.applyEditsToShadow = function ( edits, shadow ) {
        var doc, diffs, patches;

        doc = typeof shadow.doc.content === 'string' ? shadow.doc.content : JSON.stringify( shadow.doc.content );
        diffs = this._asDiffMatchPathDiffs( edits.diffs );
        patches = dmp.patch_make( doc, diffs );

        return dmp.patch_apply( patches, doc );
    };

    this.patchDocument = function( shadow ) {
        var doc, diffs, patches, patchApplied;

        // first get the document based on the shadowdocs ID
        doc = this.getDocument( shadow.id );

        // diff the doc and shadow and patch that shizzel
        diffs = dmp.diff_main( JSON.stringify( doc.content ), JSON.stringify( shadow.doc.content ) );

        patches = dmp.patch_make( JSON.stringify( doc.content ), diffs );

        patchApplied = dmp.patch_apply( patches, JSON.stringify( doc.content ) );

        //save the newly patched document
        doc.content = JSON.parse( patchApplied[0] );

        this._saveDocument( doc );

        //return the applied patch?
        console.log('patches: ', patchApplied);
        return patchApplied;
    };

    /**
        Performs the client side patch process.


        @param patchMsg the patch message that is sent from the server

        @example:

            {
                "msgType":"patch",
                "id":"12345",
                "clientId":"3346dff7-aada-4d5f-a3da-c93ff0ffc472",
                "edits":[{
                        "clientVersion":0,
                        "serverVersion":0,
                        "checksum":"5f9844b21c298ea1f3ed7bf37f96e42df03395b",
                        "diffs":[{
                            "operation":"UNCHANGED","text":"I'm a Je"},
                            {"operation":"DELETE","text":"di"
                        }]
                }]
            }"
    */
    this.patch = function( doc ) {
        // Flow is based on the server side
        // patch the shadow
        var patchedShadow = this.patchShadow( doc );
        // Then patch the document
        this.patchDocument( patchedShadow );
        // then save backup shadow
        this._saveShadowBackup( patchedShadow );

    };

    /**
        Do we need this? Looking at the server side engine When something comes in,  we first try to patch the shadow will all the edits, then if successful, we just patch the doc with the shadow
    */
    // this.applyEditsToDoc = function ( patchMsg ) {
    //     var i, patches, edit, diffs,
    //         doc = JSON.stringify( this.getDocument( patchMsg.id ).content),
    //         edits = patchMsg.edits;

    //     for (i = 0; i < edits.length; i++) {
    //         edit = edits[i];
    //         diffs = this._asDiffMatchPathDiffs( edit.diffs );
    //         patches = dmp.patch_make( doc, diffs );
    //         patches.push( dmp.patch_apply( patches, doc ) );
    //     }
    //     console.log('patches:', patches);
    //     return patches;
    // };

    this._saveDocument = function( doc ) {
        dm.stores.docs.save( doc );
    };

    this._saveShadow = function( doc ) {
        var shadow = { id: doc.id, serverVersion: doc.serverVersion || 0, clientId: doc.clientId, clientVersion: doc.clientVersion || 0, doc: doc.doc ? doc.doc : doc };
        dm.stores.shadows.save( shadow );
    };

    this._saveShadowBackup = function( doc ) {
        var backup = { id: doc.id, clientVersion: 0, doc: doc };
        dm.stores.backups.save( backup );
    };

    this.getDocument = function( id ) {
        return dm.stores.docs.read( id )[0];
    };

    this.getShadow = function( id ) {
        return dm.stores.shadows.read( id )[0];
    };

    this.getBackup = function( id ) {
        return dm.stores.backups.read( id )[0];
    };

};
