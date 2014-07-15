(function() {

    module( 'Sync Engine test' );

    test ( 'Sync.Engine should support creation without the new keyword', function() {
        var engine = Sync.Engine();
        ok( engine , 'Should be no problem not using new when creating' );
    });

    test( 'add document', function() {
        var engine = Sync.Engine(), doc = { id: 1234, clientId: 'client1', content: { name: 'Fletch' } };
        engine.addDocument( { id: 1234, clientId: 'client1', content: { name: 'Fletch' } } );
        var actualDoc = engine.getDocument( 1234 );
        equal( actualDoc.id, 1234, 'Document id should match' );
    });

    test( 'diff document', function() {
        var engine = Sync.Engine();
        var doc = { id: 1234, clientId: 'client1', content: { name: 'Fletch' } };
        engine.addDocument( doc );

        // update the name field
        doc.content.name = 'Mr.Poon';

        var patchMsg = engine.diff( doc );
        equal ( patchMsg.msgType, 'patch', 'The message type should be "patch"');
        equal ( patchMsg.id, 1234, 'document id should be 1234');
        equal ( patchMsg.clientId, 'client1', 'clientId should be client1');

        var edit = patchMsg.edits[0];
        equal ( edit.clientVersion, 0, 'version should be zero');
        equal ( edit.serverVersion, 0, 'version should be zero');
        equal ( edit.checksum, '', 'checksum is currently not implemented.');

        var diffs = edit.diffs;
        ok( diffs instanceof Array, 'diffs should be an array of tuples' );
        ok( diffs.length == 4, 'there should be 4 diff tuples generated');
        equal ( diffs[0].operation, 'UNCHANGED', 'operation should be UNCHANGED');
        equal ( diffs[0].text, '{"name":"', 'should not change the "name" field');
        equal ( diffs[1].operation, 'DELETE' ,'operation should be DELETE');
        equal ( diffs[1].text, 'Fletch', 'Fletch was the name before the update');
        equal ( diffs[2].operation, 'ADD', 'operation should be ADD');
        equal ( diffs[2].text, 'Mr.Poon', 'Mr.Poon is the new name');
        equal ( diffs[3].operation, "UNCHANGED", 'operation should be UNCHANGED');
        equal ( diffs[3].text, '"}', 'closing bracket');
    });

    test( 'patch document', function() {
        var engine = Sync.Engine();
        var doc = { id: 1234, clientId: 'client1', content: {name: 'Fletch' } };
        engine.addDocument( doc );

        // update the name field
        doc.content.name = 'Mr.Poon';

        var patches = engine.patch( doc );
        var patch = patches[1];
        equal( patch[1][0], true, 'patch should have been successful.' );
        equal( patch[0], '{"name":"Mr.Poon"}', 'name should have been updated to Mr.Poon' );
    });

    test( 'patch two documents', function() {
        var engine = Sync.Engine();
        var content = {name: 'Fletch' };
        var doc = { id: 1234, clientId: 'client1', content: content };
        engine.addDocument( doc );

        var doc2 = { id: 1234, clientId: 'client2', content: content };
        engine.addDocument( doc2 );

        // update the name field
        doc.content.name = 'Mr.Poon';

        var patches = engine.patch( doc );
        var patch = patches[1];
        equal( patch[1][0], true, 'patch should have been successful.' );
        equal( patch[0], '{"name":"Mr.Poon"}', 'name should have been updated to Mr.Poon' );

        doc2.content.name = 'Dr.Rosen';

        var patches2 = engine.patch( doc2 );
        var patch2 = patches2[1];
        equal( patch2[1][0], true, 'patch should have been successful.' );
        equal( patch2[0], '{"name":"Dr.Rosen"}', 'name should have been updated to Dr.Rosen' );

    });

    test( 'patch shadow', function() {
        var engine = Sync.Engine();
        var content = {name: 'Fletch' };
        var doc = { id: 1234, clientId: 'client1', content: content };
        engine.addDocument( doc );
        doc.content.name = 'John Coctolstol';

        var patchMsg = engine.diff( doc );
        console.log('patchMsg', patchMsg);

        var shadow = engine.patchShadow( patchMsg );
        console.log(shadow);
        equal( shadow.doc.content, '{"name":"John Coctolstol"}', 'name should have been updated to John Coctolstol' );
        equal( shadow.serverVersion, 1, 'Server version should have been updated.' );
        equal( shadow.clientVersion, 0, 'Client version should not have been updated.' );
    });

    test( 'patch document', function() {
        var engine = Sync.Engine();
        var content = {name: 'Fletch' };
        var doc = { id: 1234, clientId: 'client1', content: content };
        engine.addDocument( doc );
        doc.content.name = 'John Coctolstol';
        var patchMsg = engine.diff( doc );

        var doc = engine.patchDocument( patchMsg );
        console.log ( 'doc?', doc );
        equal( doc.content, '{"name":"John Coctolstol"}', 'name should have been updated to John Coctolstol' );
    });

})();
