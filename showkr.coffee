queryString = (obj) ->
    s = (k + '=' + v for k, v of obj)
    s.join('&')


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
        @el = $('<div class="photo">')[0]

    url: ->
        "http://farm#{@data.farm}.staticflickr.com/#{@data.server}/" +
            "#{@data.id}_#{@data.secret}_z.jpg"

    flickrUrl: ->
        "http://www.flickr.com/photos/#{@owner}/#{@data.id}/in/set-#{@set}/"

    render: ->
        @el.innerHTML = @template(this)
        this


class @Showkr
    base: 'http://api.flickr.com/services/rest/'

    constructor: ({@key, el}) ->
        @el = $(el)[0]
        @go()

    api: (options, cb) ->
        options.api_key = @key
        options.format = 'json'
        options.jsoncallback = '?'
        $.getJSON(@base + '?' + queryString(options), cb)

    go: ->
        form = $('form')

        if location.hash
            form.remove()
            @set = location.hash.slice(1)
            @fetchPhotos()
        else
            form.on 'submit', _.bind(@catchUrl, this)

    catchUrl: (e) ->
        e.preventDefault()
        url = $('input[name=url]', e.target).val()
        set = url.match(/\/sets\/([^\/]+)/)[1]
        location.hash = set
        location.reload()

    fetchPhotos: ->
        @api({method: 'flickr.photosets.getPhotos', photoset_id: @set},
             _.bind(@renderPhotos, @))

    renderPhotos: ({photoset}) ->
        for item in photoset.photo
            photo = new Photo(item, {owner: photoset.owner, set: photoset.id})
            @el.appendChild(photo.render().el)
        console.log photoset
