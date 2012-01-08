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


class CommentSliceView extends Backbone.View
    pageBy: 5
    currentPage: 1

    initialize: ({@comments})->
        @comments.bind 'reset', @render, this
        @bind 'page', @render, this

    render: ->
        @el.innerHTML = ''
        frag = document.createDocumentFragment()
        for comment in @getPage().list
            frag.appendChild(@addOne(comment))
        @el.appendChild(frag)
        this

    addOne: (comment) ->
        view = new CommentView(model: comment)
        return view.render().el

    getPage: ->
        start = (@currentPage - 1) * @pageBy
        end = start + @pageBy
        if end > @comments.length
            end = @comments.length
        return {
            length: @comments.length
            pageBy: @pageBy
            page: @currentPage
            start: start
            end: end
            list: @comments.models.slice(start, end)
            hasNext: @currentPage * @pageBy < @comments.length
            hasPrev: @currentPage > 1
        }

    prevPage: ->
        if @currentPage > 1
            @currentPage -= 1
            @trigger 'page', this

    nextPage: ->
        if @currentPage * @pageBy < @comments.length
            @currentPage += 1
            @trigger 'page', this


class Paginator extends Backbone.View
    template: require 'templates/paginator.eco'

    events:
        'click .prev': 'prevPage'
        'click .next': 'nextPage'
        'click .disabled': 'disabled'

    render: (view) ->
        @el.innerHTML = @template(view.getPage())

    disabled: (e) ->
        e.preventDefault()

    prevPage: (e) ->
        e.preventDefault()
        @trigger 'prev'

    nextPage: (e) ->
        e.preventDefault()
        @trigger 'next'


class CommentListView extends Backbone.View
    template: require 'templates/comment-wrapper.eco'

    initialize: ({@comments, @withHeader})->
        @comments.bind 'reset', @render, this

    render: ->
        return unless @comments.length

        @el.innerHTML = @template
            withHeader: @withHeader
            comments: @comments
            pageBy: CommentListView::pageBy
        @paginator = new Paginator(el: @$('.pagination')[0])
        @inner = new CommentSliceView
            el: @$('ul.comments')[0]
            comments: @comments

        @inner.bind 'page', @paginator.render, @paginator
        @paginator.bind 'next', @inner.nextPage, @inner
        @paginator.bind 'prev', @inner.prevPage, @inner

        @inner.trigger 'page', @inner

        this


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
