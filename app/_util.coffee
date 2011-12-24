_ = require 'underscore'
Backbone = require 'backbone'
exports = {}


exports.urlParameters = ->
    data = {}
    for item in location.search.slice(1).split('&')
        [k, v] = item.split('=')
        data[k] = decodeURIComponent(v)
    data


class exports.Model extends Backbone.Model
    @field: (name) ->
        @::[name] = (value) ->
            if not arguments.length
                @get(name)
            else
                data = {}
                data[name] = value
                @set(data)


class exports.View extends Backbone.View
    constructor: ->
        if @template
            @template = _.template($(@template).html())
        super

    render: ->
        @el.innerHTML = @template(@model)
        this

provide 'util', exports
