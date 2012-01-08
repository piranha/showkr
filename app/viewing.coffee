$ = ender
_ = require 'underscore'
Backbone = require 'backbone'
{View} = require 'util'
{Set} = require 'models'


class CommentView extends View
    tagName: 'li'
    className: 'comment'
    template: require 'templates/comment.eco'

    initialize: ({number}) ->
        @el.id = @model.id
        @model.bind 'change', @render, this


class CommentListView extends Backbone.View
    tagName: 'ul'
    className: 'comments'
    template: require 'templates/comment-list.eco'

    events:
        'click .more': 'renderMore'

    initialize: ({@comments, @withHeader})->
        @comments.bind 'reset', @addAll, this

    render: ->
        @el.innerHTML = @template
            withHeader: @withHeader
            comments: @comments
        this

    renderMore: (e) ->
        e.preventDefault()
        @render()
        @addAll(@comments, {}, true)

    addAll: (comments, options, full) ->
        @render()
        frag = document.createDocumentFragment()
        for comment, i in comments.models
            if i > 5 and not full
                a = @make 'a', {href: '#'}, 'More comments...'
                more = @make 'li', {class: 'more'}, ''
                more.appendChild(a)
                frag.appendChild more
                break
            @addOne(comment, frag)
        @el.appendChild(frag)

    addOne: (comment, frag) ->
        view = new CommentView(model: comment)
        frag.appendChild view.render().el


class PhotoView extends View
    className: 'photo'
    template: require 'templates/photo.eco'

    events:
        'click h3 > .idlink': 'scrollTo'

    initialize: ({@set, number}) ->
        @model.number(number + 1)
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
        if window.showkr
            showkr.navigate("#{@model.collection.set.id}/#{@model.id}", false)


class SetView extends View
    template: require 'templates/set.eco'

    events:
        'click .embed': 'showEmbed'

    keys:
        'j': 'nextPhoto'
        'k': 'prevPhoto'
        'down': 'nextPhoto'
        'up': 'prevPhoto'

    initialize: ({@config}) ->
        @model = Set.getOrCreate(@id)
        @views = {}

        @model.photolist().bind 'reset', @addAll, this
        $(window).on 'load', _.bind(@scrollTo, this)
        if window.showkr
            @model.bind 'change:title', showkr.addToHistory, showkr
        # FIXME I should re-render here or use some data-to-html binding stuff
        # instead of this
        @model.bind 'change:title', =>
            @$('[rel=title]').html(@model.title())
        @model.bind 'change:description', =>
            @$('[rel=description]').html(@model.description())

        for key, fn of @keys
            $.key key, _.bind(@[fn], @)

        @model.fetch()

    embed: ->
        ("<script src='http://showkr.org/embed.js' " +
         "data-set='#{@id}' data-title='false'></script>")

    context: ->
        model: @model
        config: @config

    render: ->
        super
        if not @model.comments().length
            @model.comments().fetch()
        @comments = new CommentListView
            comments: @model.comments()
            withHeader: true
        this

    addAll: (photos) ->
        _.each @views, (v) -> v.remove()
        @views = {}
        frag = document.createDocumentFragment()
        for photo, i in photos.models
            @addOne(photo, i, frag)
        @scrollTo(@targetId) if @targetId
        frag.appendChild @comments.el
        @el.appendChild(frag)

    addOne: (photo, number, frag) ->
        view = new PhotoView(model: photo, number: number)
        @views[photo.id] = view
        frag.appendChild view.render().el

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
            if top > window.scrollY
                foundNext = true
        if photo
            if window.showkr
                showkr.navigate("#{@model.id}/#{photo.id}", false)
            window.scroll(0, top)

    prevPhoto: (e) ->
        e.preventDefault()
        for photo in @model.photolist().models
            {top} = $(photo.view.el).offset()
            if top >= window.scrollY
                break
            previous = photo
        if previous
            if window.showkr
                showkr.navigate("#{@model.id}/#{previous.id}", false)
            window.scroll(0, $(previous.view.el).offset().top)
        else
            if window.showkr
                showkr.navigate("#{@model.id}", false)
            window.scroll(0, 0)


provide 'viewing', {SetView}
