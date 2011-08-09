var triface = function() {
    var rpc = new easyXDM.Rpc({
        remote: "http://api.triface.local/cors/"
    }, {
        remote: {
            request: {}
        }
    });

    var api = {};

    api.request = function(request) {
        var success = request.success || function(response) {};
        var error = request.error || function(response) {console.log(response);};

        rpc.request(request, function(response) {
            success(eval(response.data));
        }, function(response) {
            error(response);
        });
    };

    api.get = function(request) {
        request.method = 'GET';
        api.request(request);
    };

    api.post = function(request) {
        request.method = 'POST';
        api.request(request);
    };

    return {
        api: api
    };
}();