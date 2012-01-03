_ = require 'underscore'
Backbone = require 'backbone'
exports = {}

MONTHS = ('January February March April May June July August September ' +
          'October November December').split(' ')

$.ender({
    attr: (k, v) ->
        if typeof k != 'string'
            for n of k
                @attr(n, k[n])
            return
        for el in this
            el[k] = v
    html: (v) ->
        if not v
            return this[0].innerHTML
        for el in this
            el.innerHTML = v
}, true)


exports.addOrPromote = (list, value) ->
    for item, i in list
        # something strange happens here sometimes with `undefined`
        if item == undefined or item[0] == value[0]
            list.splice(i, 1)
    list.unshift(value)
    return list


exports.formatDate = (d) ->
    "#{d.getDate()} #{MONTHS[d.getMonth()]} #{d.getFullYear()}"


class exports.Model extends Backbone.Model
    @field: (name) ->
        @::[name] = (value) ->
            if not arguments.length
                value = @get(name)
                if not value
                    return value
                if typeof value._content != 'undefined'
                    return value._content
                return value
            else
                data = {}
                data[name] = value
                @set(data)


class exports.View extends Backbone.View
    render: ->
        @el.innerHTML = @template(@model)
        this


provide 'util', exports
