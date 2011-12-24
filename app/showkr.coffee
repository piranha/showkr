_ = require 'underscore'
Backbone = require 'backbone'
{urlParameters, Model, View} = require 'util'


class Comment extends Model
    @field '_content'
    @field 'author'
    @field 'authorname'
    @field 'datecreate'
    @field 'iconfarm'
    @field 'iconserver'
    @field 'permalink'

    content: -> @_content()
    date: -> @datecreate()

    authorlink: ->
        "http://flickr.com/photos/#{@author()}/"

    icon: ->
        if @iconserver() == '0'
            return "http://www.flickr.com/images/buddyicon.jpg"
        "http://farm#{@iconfarm()}.static.flickr.com/#{@iconserver()}" +
            "/buddyicons/#{@author()}.jpg"

class Comments extends Backbone.Collection
    model: Comment

    initialize: (models, {@photo}) ->

    sync: (method, coll, {success, error}) ->
        if method != 'read'
            return alert 'wtf'

        id = @photo.id
        app.api
            method: 'flickr.photos.comments.getList'
            photo_id: @photo.id
            callback: (data) ->
                (if data.stat == 'ok' then success else error)(data)

    parse: (response) ->
        return response.comments.comment


class Photo extends Model
    # @field 'id'
    @field 'secret'
    @field 'farm'
    @field 'server'
    @field 'title'
    @field 'comments'

    initialize: ->
        if not @id
            console.log 'id undefined', @cid, @
            return
        @comments(new Comments(null, {photo: this}))
        @comments().fetch()

    # s  small square 75x75
    # t  thumbnail, 100 on longest side
    # m  small, 240 on longest side
    # -  medium, 500 on longest side
    # z  medium 640, 640 on longest side
    # b  large, 1024 on longest side*
    # o  original image, either a jpg, gif or png, depending on source format
    url: (size='z') ->
        "http://farm#{@farm()}.staticflickr.com/#{@server()}/" +
            "#{@id}_#{@secret()}_#{size}.jpg"

    flickrUrl: ->
        "http://www.flickr.com/photos/#{@owner()}/#{@id}/in/set-#{@setId()}/"

    owner: ->
        @collection.set.owner()

    setId: ->
        @collection.set.id


class Photos extends Backbone.Collection
    model: Photo

    initialize: (models, {@set}) ->


class Set extends Model
    # @field 'id'
    @field 'owner'
    @field 'ownername'

    @field 'photos'

    initialize: ->
        @photos(new Photos(null, {set: this}))

    sync: (method, coll, {success, error}) ->
        if method != 'read'
            return alert 'wtf'

        app.api
            method: 'flickr.photosets.getPhotos'
            photoset_id: @id
            callback: (data) ->
                (if data.stat == 'ok' then success else error)(data.photoset)

    parse: (photoset) ->
        {photo} = photoset
        delete photoset.photo
        @photos().reset(photo)
        return photoset


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


class SetView extends Backbone.View
    keys:
        'j': 'nextPhoto'
        'k': 'prevPhoto'
        'down': 'nextPhoto'
        'up': 'prevPhoto'

    initialize: ->
        @set = new Set({id: @id})
        @views = {}

        @set.photos().bind 'reset', @addAll, this
        $(window).on 'load', _.bind(@scrollTo, this)

        for key, fn of @keys
            $.key key, _.bind(@[fn], @)

        @set.fetch()

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
        e.preventDefault()
        for photo in @set.photos().models
            if foundNext
                break
            {top} = $(photo.view.el).offset()
            if top >= (window.scrollY - 25)
                foundNext = true
        app.navigate("#{@set.id}/#{photo.id}", true)

    prevPhoto: (e) ->
        e.preventDefault()
        for photo in @set.photos().models
            {top} = $(photo.view.el).offset()
            if top >= (window.scrollY - 25)
                break
            previous = photo
        if previous
            app.navigate("#{@set.id}/#{previous.id}", true)


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
    base: 'http://api.flickr.com/services/rest/'

    routes:
        '': 'index'
        ':set': 'set',
        ':set/:photo': 'set'

    initialize: ({@key}) ->
        @el = $('#main')

    index: ->
        form = new Form()
        @el.append(form.render().el)

    set: (set, photo) ->
        if @setview?.id != set
            @setview = new SetView({id: set})
            @el.append(@setview.render().el)
        if photo
            @setview.scrollTo(photo)

    api: (options) ->
        callback = options.callback
        delete options.callback

        options.api_key = @key
        options.format = 'json'
        $.ajax
            url: @base + '?' + $.toQueryString(options) + '&jsoncallback=?'
            type: 'jsonp'
            success: callback
