#!/usr/bin/env python

import sys
import cssutils

PREFIX = '.showkr'
cssutils.log.enabled = False
# cssutils.ser.prefs.keepComments = False
# cssutils.ser.prefs.indent = ''
# cssutils.ser.prefs.spacer = ''
# cssutils.ser.prefs.listItemSpacer = ''
# cssutils.ser.prefs.propertyNameSpacer = ''

def main(fn):
    style = cssutils.parseString(open(fn).read())
    for rule in style.cssRules:
        if not hasattr(rule, 'selectorList'):
            continue
        for s in rule.selectorList:
            if s.selectorText in ('html', 'body'):
                s.selectorText = PREFIX
            else:
                s.selectorText = PREFIX + ' ' + s.selectorText
    text = style.cssText
    print text.replace('\x00', '\\0')


if __name__ == '__main__':
    if len(sys.argv) < 1:
        print 'pass filename as parameter'
    else:
        main(sys.argv[1])
