#!/usr/bin/env python

import sys
import cssutils

PREFIX = 'div#_showkr'
cssutils.log.enabled = False
# cssutils.ser.prefs.keepComments = False
# cssutils.ser.prefs.indent = ''
# cssutils.ser.prefs.spacer = ''
# cssutils.ser.prefs.listItemSpacer = ''
# cssutils.ser.prefs.propertyNameSpacer = ''

def main(fn):
    style = cssutils.parseFile(fn)
    for rule in style.cssRules:
        if not hasattr(rule, 'selectorList'):
            continue
        for selector in rule.selectorList:
            selector.selectorText = PREFIX + ' ' + selector.selectorText
    print style.cssText


if __name__ == '__main__':
    if len(sys.argv) < 1:
        print 'pass filename as parameter'
    else:
        main(sys.argv[1])
