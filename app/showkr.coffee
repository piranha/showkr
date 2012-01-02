_ = require 'underscore'
Backbone = require 'backbone'
{addOrPromote, View} = require 'util'
{Set} = require 'models'


class CommentView extends View
    tagName: 'li'
    className: 'comment'
    template: '#comment-template'

    initialize: ->
        @el.id = @model.id
        @model.bind 'change', @render, this


class CommentListView extends Backbone.View
    initialize: ({@comments})->
        @comments.bind 'reset', @addAll, this
        # NOTE: not sure the view will be created before comments are fetched...
        # if @comments.length
        #     @addAll(@comments)

    addAll: (comments) ->
        for comment in comments.models
            @addOne(comment)

    addOne: (comment) ->
        view = new CommentView(model: comment)
        @el.appendChild view.render().el


class PhotoView extends View
    className: 'photo'
    template: '#photo-template'

    events:
        'click h3 > .idlink': 'scrollTo'

    initialize: ({@set}) ->
        @el.id = @model.id
        @model.view = this
        @model.bind 'change', @render, this

    render: ->
        super
        @comments = new CommentListView
            el: $('.comments', @el)[0]
            comments: @model.comments()
        this

    scrollTo: (e) ->
        e.preventDefault()
        window.scroll(0, $(@el).offset().top)
        app.navigate("#{@model.collection.set.id}/#{@model.id}", false)


class SetView extends View
    template: '#set-template'
    keys:
        'j': 'nextPhoto'
        'k': 'prevPhoto'
        'down': 'nextPhoto'
        'up': 'prevPhoto'

    initialize: ->
        @model = new Set({id: @id})
        @views = {}

        @model.photos().bind 'reset', @addAll, this
        $(window).on 'load', _.bind(@scrollTo, this)
        @model.bind 'change:title', app.addToHistory, app
        # FIXME ugly hack! :(
        # I should re-render here or use some data-to-html binding stuff instead
        # of this crap
        @model.bind 'change:title', =>
            $('h1', @el).html(@model.title())

        for key, fn of @keys
            $.key key, _.bind(@[fn], @)

        @model.fetch()

    addAll: (photos) ->
        _.each @views, (k, v) -> v.remove()
        @views = {}
        for photo in photos.models
            @addOne(photo)
        @scrollTo(@targetId) if @targetId

    addOne: (photo) ->
        view = new PhotoView(model: photo)
        @views[photo.id] = view
        @el.appendChild view.render().el

    scrollTo: (id) ->
        if typeof id == 'string'
            @targetId = id
        view = @views[@targetId]
        return unless view
        window.scroll(0, $(view.el).offset().top)

    nextPhoto: (e) ->
        console.log 'calling this again', e
        e.preventDefault()
        for photo in @model.photos().models
            if foundNext
                break
            {top} = $(photo.view.el).offset()
            if top >= (window.scrollY - 25)
                foundNext = true
        if photo
            app.navigate("#{@model.id}/#{photo.id}", true)

    prevPhoto: (e) ->
        e.preventDefault()
        for photo in @model.photos().models
            {top} = $(photo.view.el).offset()
            if top >= (window.scrollY - 25)
                break
            previous = photo
        if previous
            app.navigate("#{@model.id}/#{previous.id}", true)
        else
            app.navigate("#{@model.id}", false)
            window.scroll(0, 0)


class Form extends Backbone.View
    tagName: 'form'

    events:
        'submit': 'submit'

    initialize: ->
        @template = _.template($('#form-template').html())

    render: ->
        @el.innerHTML = @template()
        this

    submit: (e) ->
        e.preventDefault()
        {url} = $(e.target).serialize(type: 'map')
        if url.match(/^\d+$/)
            set = url
        else if url.match(/\/sets\/([^\/]+)/)
            set = url.match(/\/sets\/([^\/]+)/)[1]
        else
            return alert 'something is wrong in your input'

        app.navigate(set, true)


class @Showkr extends Backbone.Router
    routes:
        '': 'index'
        ':set': 'set'
        ':set/:photo': 'set'

    initialize: ->
        @el = $('#main')
        $.key 'shift+/', _.bind(@showHelp, @)

    # ## Views

    index: ->
        @el.children().hide()
        form = new Form()
        @el.append form.render().el
        $(form.el).show()

    set: (set, photo) ->
        if @setview?.id != set
            @el.children().hide()
            @setview = new SetView({id: set})
            @el.append @setview.render().el
        if photo
            @setview.scrollTo(photo)

    # ## Helpers

    showHelp: ->
        $('#help').overlay().open()

    addToHistory: (set) ->
        history = JSON.parse(localStorage.showkr or '[]')
        history = addOrPromote(history, [set.id, set.title()])
        history = history[..20]
        localStorage.showkr = JSON.stringify(history)

    getHistory: ->
        JSON.parse(localStorage.showkr or '[]')
