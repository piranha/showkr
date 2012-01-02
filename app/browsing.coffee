_ = require 'underscore'
Backbone = require 'backbone'
{View, Model} = require 'util'
{User} = require 'models'


class BrowsingSetView extends View
    template: '#browsing-set-template'


class UserView extends View
    template: '#user-template'

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
        for set in sets.models
            @addOne(set)

    addOne: (set) ->
        view = new BrowsingSetView(model: set)
        @el.appendChild view.render().el


provide 'browsing', {UserView}
