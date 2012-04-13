_.capitalize = function(string) {
  var bits = string.split(/[_ ]+/);
  var shaped = _.map(bits, function(bit) {
    var low = bit.toLowerCase();
    return low.charAt(0).toUpperCase() + low.slice(1);
  });
  return shaped.join(' ');
};

_.slugify = function(string) {
  var bits = string.split(/[^a-zA-Z]+/);
  var shaped = _.map(bits, function(bit) {
    return bit.toLowerCase();
  });
  return shaped.join('_');
}

var caribou = function() {
  var LOCATION = window.location.toString();
  var REMOTE = 'http://localhost:33443';
  if (LOCATION.match(/^https?:\/\/admin/)) {
    REMOTE = LOCATION.replace(/^(https?:\/\/)(admin)(\.[^\/]+).*/, "$1api$3")
  }
  console.log(REMOTE);
  var rpc = new easyXDM.Rpc({
    remote: REMOTE+"/cors/"
  }, {
    remote: {
      request: {}
    }
  });

  var upload = function(success) {
    return new easyXDM.Rpc({
      remote: REMOTE+"/upload_rpc.html",
      swf: REMOTE+"/easyxdm.swf",
      onReady: function() {
        //display the upload form
        var form = $('#file_upload')[0];
        console.log(form);
        if (form) {
          form.action = REMOTE + "/upload";
          // var button = document.getElementById("btnSubmit");
          
          // form.onsubmit = function(){
          //     button.disabled = "disabled";
          // };
        }
      }
    }, {
      local: {
        returnUploadResponse: function(response) {
          console.log(response);
          if (success) {
            success(response);
          }
        }
      }
    });
  };

  var api = {};
  var sherpa = new Sherpa.Router();

  var getPath = function() {
    var state = History.getState().hash.split('?');
    var path = state[0];
    var query = {};

    if (state[1]) {
      var params = state[1].split('&');
      if (params[0] === '') {
        params = params.slice(1);
      }

      query = _.reduce(params, function(args, param) {
        var parts = param.split('=');
        args[parts[0]] = parts[1];
        return args;
      }, {});
    }

    return {path: path, query: query};
  };

  api.upload = upload;
  api.request = function(request) {
    var success = request.success || function(response) {};
    var error = request.error || function(response) {console.log(JSON.parse(response));};

    rpc.request(request, function(response) {
      var data = JSON.parse(response.data);
      var code = parseInt(data.meta.status);

      if (code === 403) {
        go('/login')
      } else {
        success(data);
      }
    }, function(response) {
      error(response);
    });
  };

  api.upload = upload;

  api.get = function(request) {
    request.method = 'GET';
    api.request(request);
  };

  api.post = function(request) {
    request.method = 'POST';
    api.request(request);
  };

  api.put = function(request) {
    request.method = 'PUT';
    api.request(request);
  };

  api.delete = function(request) {
    request.method = 'DELETE';
    api.request(request);
  };

  var go = function(path) {
    var state = History.getState();
    var trodden = _.last(state.cleanUrl.match(/http:\/\/[^\/]+(.*)/));

    if (path === trodden) {
      act();
    } else {
      History.pushState(path, path, path);
    }
  };

  var routing = {
    actions: {},

    add: function(path, name, action) {
      sherpa.add(path).to(name);
      this.actions[name] = action;
    },

    match: function(path, query) {
      var match = sherpa.recognize(path);
      var action = this.actions[match.destination];
      return function() {
        return action(match.params, query);
      };
    },

    action: function() {
      var match = getPath();
      return this.match(match.path, match.query);
    }
  };

  var models = {};
  var modelNames = [];

  var resetModels = function(success) {
    api.get({
      url: "/model",
      data: {include: "fields.link"},
      success: function(response) {
        _.each(response.response, function(model) {
          for (var i = 0; i < model.fields.length; i++) {
            var target_id = model.fields[i].target_id;
            model.fields[i].target = target_id ? function(target_id) {
              return function() {
                return models[target_id];
              };
            }(target_id) : function() {};
          }

          models[model.id] = model;
          models[model.slug] = model;
          modelNames.push(model.slug);
        });

        success();
      }
    });
  };

  var modelFieldTypes = [];
  var retrieveFieldTypes = function() {
    api.get({
      cache: false,
      url: "/type-specs.json",
      success: function(response) {
        modelFieldTypes = response.response;
      }
    });
  };

  var act = function() {
    var action = routing.action();
    action();
  };

  var formData = function(selector) {
    var data = {};
    var verbose = $(selector).serializeArray();

    for (var i = 0; i < verbose.length; i++) {
      if (verbose[i].value && !(verbose[i].value === '')) {
        data[verbose[i].name] = verbose[i].value;
      }
    }

    var checks = $(selector + " input:checkbox");
    for (i = 0; i < checks.length; i++) {
      data[checks[i].name] = checks[i].checked;
    }

    // var files = $(selector + " input:file");
    // for (i = 0; i < files.length; i++) {
    //     var file = files[i].files[0];
    //     var fd = new FormData();
    //     fd.append('file', file);
    //     data[files[i].name] = checks[i].checked;
    // }

    return data;
  };

  var init = function(success) {
    window.onstatechange = act;
    resetModels(act);
    retrieveFieldTypes();
  };

  return {
    init: init,
    api: api,
    go: go,
    act: act,
    models: models,
    modelNames: modelNames,
    modelFieldTypes: function() {return modelFieldTypes;},
    routing: routing,
    formData: formData,
    resetModels: resetModels,
    remoteAPI: REMOTE
  };
}();
