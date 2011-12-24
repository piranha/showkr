_ = require 'underscore'
Backbone = require 'backbone'
exports = {}


exports.addOrPromote = (list, value) ->
    for item, i in list
        if item[0] == value[0]
            list.splice(i, 1)
    list.unshift(value)
    return list

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
