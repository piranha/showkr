$ = ender
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
        if typeof v == 'undefined'
            return this[0].getAttribute(k)
        for el in this
            el.setAttribute(k, v)
        this

    html: (v) ->
        if not v
            return this.length and this[0].innerHTML
        for el in this
            el.innerHTML = v
        this

    val: (v) ->
        if not v
            return this.length and this[0].value
        for el in this
            el.value = v
        this

    offset: ->
        el = this[0]
        width = el.offsetWidth
        height = el.offsetHeight
        top = el.offsetTop
        left = el.offsetLeft

        while el = el.offsetParent
            top += el.offsetTop
            left += el.offsetLeft

        return {top, left, height, width}

    data: ->
        data = {}
        for attr in this[0].attributes
            if attr.name[..4] == 'data-'
                data[attr.name[5..]] = if attr.value == 'false'
                        false
                    else if attr.value == 'true'
                        true
                    else
                        attr.value
        return data

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
    context: ->
        @model

    render: ->
        @el.innerHTML = @template(@context())
        this

    showEmbed: (e) ->
        e.preventDefault()
        input = @make 'input', value: @embed()
        parent = e.target.parentNode
        old = parent.replaceChild(input, e.target)
        input.focus()
        $(input).bind 'blur', ->
            parent.replaceChild(old, input)


provide 'util', exports
