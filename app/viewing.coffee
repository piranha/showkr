_ = require 'underscore'
Backbone = require 'backbone'
{View} = require 'util'
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

    render: ->
        super
        if not @model.comments().length
            @model.comments().fetch()
        @comments = new CommentListView
            el: @$('.comments')[0]
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
        @model = Set.getOrCreate(@id)
        @views = {}

        @model.photolist().bind 'reset', @addAll, this
        $(window).on 'load', _.bind(@scrollTo, this)
        @model.bind 'change:title', app.addToHistory, app
        # FIXME ugly hack! :(
        # I should re-render here or use some data-to-html binding stuff instead
        # of this crap
        @model.bind 'change:title', =>
            @$('h1').html(@model.title())
        @model.bind 'change:description', =>
            @$('small').html(@model.description())

        for key, fn of @keys
            $.key key, _.bind(@[fn], @)

        @model.fetch()

    addAll: (photos) ->
        _.each @views, (v) -> v.remove()
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
        e.preventDefault()
        for photo in @model.photolist().models
            if foundNext
                break
            {top} = $(photo.view.el).offset()
            if top >= (window.scrollY - 25)
                foundNext = true
        if photo
            app.navigate("#{@model.id}/#{photo.id}", true)

    prevPhoto: (e) ->
        e.preventDefault()
        for photo in @model.photolist().models
            {top} = $(photo.view.el).offset()
            if top >= (window.scrollY - 25)
                break
            previous = photo
        if previous
            app.navigate("#{@model.id}/#{previous.id}", true)
        else
            app.navigate("#{@model.id}", false)
            window.scroll(0, 0)


provide 'viewing', {SetView}
