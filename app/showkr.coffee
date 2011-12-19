_ = require 'underscore'

urlParameters = ->
    data = {}
    for item in location.search.slice(1).split('&')
        [k, v] = item.split('=')
        data[k] = decodeURIComponent(v)
    data

class Photo
    # s  small square 75x75
    # t  thumbnail, 100 on longest side
    # m  small, 240 on longest side
    # -  medium, 500 on longest side
    # z  medium 640, 640 on longest side
    # b  large, 1024 on longest side*
    # o  original image, either a jpg, gif or png, depending on source format
    size: 'z'

    constructor: (@data, {@owner, @set}) ->
        @template = _.template($('#photo-template').html())
        @el = $("<div class='photo' id='#{@data.id}'>")[0]
        # @api({method: 'flickr.photos.comments.getList', photoset_id: @set},
        #      @renderComments)

    url: ->
        "http://farm#{@data.farm}.staticflickr.com/#{@data.server}/" +
            "#{@data.id}_#{@data.secret}_#{@size}.jpg"

    flickrUrl: ->
        "http://www.flickr.com/photos/#{@owner}/#{@data.id}/in/set-#{@set}/"

    render: ->
        @el.innerHTML = @template(this)
        this


class @Showkr
    base: 'http://api.flickr.com/services/rest/'

    constructor: ({@key, el}) ->
        @el = $(el)[0]
        @photos = {}
        @go()

    api: (options, cb) ->
        options.api_key = @key
        options.format = 'json'
        $.ajax
            url: @base + '?' + $.toQueryString(options) + '&jsoncallback=?'
            type: 'jsonp'
            success: _.bind(cb, this)

    go: ->
        form = $('form')
        {set} = urlParameters()

        if set
            form.remove()
            @set = set
            @api({method: 'flickr.photosets.getPhotos', photoset_id: @set},
                 @renderPhotos)
        else
            form.on 'submit', _.bind(@catchUrl, this)

    catchUrl: (e) ->
        e.preventDefault()
        {url} = $(e.target).serialize(type: 'map')
        if url.match(/^\d+$/)
            set = url
        else if url.match(/\/sets\/([^\/]+)/)
            set = url.match(/\/sets\/([^\/]+)/)[1]
        else
            alert 'something is wrong in your input'
            return
        location.search = "?set=#{set}"

    renderPhotos: ({photoset}) ->
        for item in photoset.photo
            photo = new Photo(item, this,
                              {owner: photoset.owner, set: photoset.id})
            @photos[item.id] = photo
            @el.appendChild(photo.render().el)
        console.log photoset

    renderComments: ->
        console.log arguments
