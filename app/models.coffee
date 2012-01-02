_ = require 'underscore'
Backbone = require 'backbone'
API = require 'api'
{addOrPromote, Model} = require 'util'
exports = {}


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
        API.commentList @photo.id, (data) ->
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

    sync: (method, coll, {success, error}) ->
        if method != 'read'
            return alert 'wtf'

        API.photoList @set.id, (data) ->
            (if data.stat == 'ok' then success else error)(data)

    parse: ({photoset}) ->
        return photoset.photo


class exports.Set extends Model
    # @field 'id'
    @field 'owner'
    @field 'ownername'
    @field 'title'
    @field 'description'

    @field 'photos'

    initialize: ->
        @photos(new Photos(null, {set: this}))

    fetch: ->
        super

    sync: (method, coll, {success, error}) ->
        photos = @photos()
        API.setInfo @id, (data) ->
            (if data.stat == 'ok' then success else error)(data)
            photos.fetch()

    parse: ({photoset}) ->
        photoset.description = photoset.description._content
        photoset.title = photoset.title._content
        delete photoset.photos # number of photos!
        return photoset


provide 'models', exports
