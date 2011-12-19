_ = require 'underscore'
Backbone = require 'backbone'
{urlParameters, Model} = require 'util'


class Photo extends Model
    # @field 'id'
    @field 'secret'
    @field 'farm'
    @field 'server'
    @field 'title'

    # @field 'owner'
    # @field 'set'

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


class @Set extends Model
    # @field 'id'
    @field 'owner'
    @field 'ownername'

    @field 'photos'

    initialize: ->
        @photos(new Photos())
        @photos().set = this

    sync: (method, coll, {success, error}) ->
        if method != 'read'
            return alert 'wtf'

        app.api {method: 'flickr.photosets.getPhotos', photoset_id: @id},
            (data) ->
                (if data.stat == 'ok' then success else error)(data)

    parse: ({photoset}) ->
        {photo} = photoset
        delete photoset.photo
        @photos().reset(photo)
        return photoset


class PhotoView extends Backbone.View
    className: 'photo'

    initialize: ({@set}) ->
        @template = _.template($('#photo-template').html())
        @el.id = @model.id

        @model.bind 'change', @render, this

    render: ->
        @el.innerHTML = @template(@model)
        this


class SetView extends Backbone.View
    initialize: ->
        @set = new Set({id: @id})

        @views = {}
        @set.photos().bind 'reset', @addAll, this
        $(window).on 'load', _.bind(@scrollTo, this)

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

    api: (options, cb) ->
        options.api_key = @key
        options.format = 'json'
        $.ajax
            url: @base + '?' + $.toQueryString(options) + '&jsoncallback=?'
            type: 'jsonp'
            success: cb
