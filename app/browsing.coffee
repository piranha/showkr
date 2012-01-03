_ = require 'underscore'
Backbone = require 'backbone'
{View, Model} = require 'util'
{User, Photo} = require 'models'


class BrowsingSetView extends View
    template: require 'templates/browsing-set.eco'
    className: 'row'

    initialize: ->
        @photo = new Photo
            id: @model.primary()
            secret: @model.secret()
            farm: @model.farm()
            server: @model.server()

    render: ->
        @el.innerHTML = @template(model: @model, photo: @photo)
        this


class UserView extends View
    template: require 'templates/user.eco'

    initialize: ({user}) ->
        if ~user.indexOf('@')
            @model = new User(id: user)
        else
            @model = new User(username: user)

        @model.fetch()

        @model.bind 'change', @render, this
        @model.sets().bind 'reset', @addAll, this

    addAll: (sets) ->
        @render()
        frag = document.createDocumentFragment()
        for set in sets.models
            @addOne(set, frag)
        @el.appendChild(frag)

    addOne: (set, frag) ->
        view = new BrowsingSetView(model: set)
        (frag or @el).appendChild view.render().el


provide 'browsing', {UserView}
