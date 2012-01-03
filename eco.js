#!/usr/bin/env node

var fs = require('fs');
var eco = require('eco');
var indent = require('eco/lib/util').indent;

function run() {
    if (process.argv.length < 4) {
        console.log('Usage: ' + process.argv[1] + ' FILE.eco MODULENAME\n');
        process.exit(0);
    }

    var infile = process.argv[2];
    var modulename = process.argv[3];
    var source = fs.readFileSync(infile, 'utf8');
    var template = indent(eco.precompile(source), 2);

    console.log('provide("' + modulename + '", ' + template.slice(2) + ');\n');
}

run();
