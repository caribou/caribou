if (!Array.prototype.indexOf)
{
  Array.prototype.indexOf = function(searchElement /*, fromIndex */)
  {
    "use strict";

    if (this === void 0 || this === null)
      throw new TypeError();

    var t = Object(this);
    var len = t.length >>> 0;
    if (len === 0)
      return -1;

    var n = 0;
    if (arguments.length > 0)
    {
      n = Number(arguments[1]);
      if (n !== n) // shortcut for verifying if it's NaN
        n = 0;
      else if (n !== 0 && n !== (1 / 0) && n !== -(1 / 0))
        n = (n > 0 || -1) * Math.floor(Math.abs(n));
    }

    if (n >= len)
      return -1;

    var k = n >= 0
          ? n
          : Math.max(len - Math.abs(n), 0);

    for (; k < len; k++)
    {
      if (k in t && t[k] === searchElement)
        return k;
    }
    return -1;
  };
}

RegExp.escape = function(text) {
  if (!arguments.callee.sRE) {
    var specials = [
      '/', '.', '*', '+', '?', '|',
      '(', ')', '[', ']', '{', '}', '\\'
    ];
    arguments.callee.sRE = new RegExp(
      '(\\' + specials.join('|\\') + ')', 'g'
    );
  }
  return text.replace(arguments.callee.sRE, '\\$1');
}

Sherpa = {
  Router: function(options) {
    this.routes = {};
    this.root = new Sherpa.Node();
    this.requestKeys = options && options['requestKeys'] || ['method'];
  },
  Path: function(route, uri) {
    this.route = route;
    var splitUri = this.pathSplit(uri);

    this.compiledUri = [];

    for (var splitUriIdx = 0; splitUriIdx != splitUri.length; splitUriIdx++) {
      if (splitUri[splitUriIdx].substring(0, 1) == ':') {
        this.compiledUri.push("params['" + splitUri[splitUriIdx].substring(1) + "']");
      } else {
        this.compiledUri.push("'" + splitUri[splitUriIdx] + "'");
      }
    }

    this.compiledUri = this.compiledUri.join('+');

    this.groups = [];

    for (var splitIndex = 0; splitIndex < splitUri.length; splitIndex++) {
      var part = splitUri[splitIndex];
      if (part == '/') {
        this.groups.push([]);
      } else if (part != '') {
        this.groups[this.groups.length - 1].push(part);
      }
    }
  },
  Route: function(router, uri) {
    this.router = router;
    this.requestConditions = {};
    this.matchingConditions = {};
    this.variableNames = [];
    var paths = [""];
    var chars = uri.split('');

    var startIndex = 0;
    var endIndex = 1;

    for (var charIndex = 0; charIndex < chars.length; charIndex++) {
      var c = chars[charIndex];
      if (c == '(') {
        // over current working set, double paths
        for (var pathIndex = startIndex; pathIndex != endIndex; pathIndex++) {
          paths.push(paths[pathIndex]);
        }
        // move working set to newly copied paths
        startIndex = endIndex;
        endIndex = paths.length;
      } else if (c == ')') {
        // expand working set scope
        startIndex -= (endIndex - startIndex);
      } else {
        for (var i = startIndex; i != endIndex; i++) {
          paths[i] += c;
        }
      }
    }

    this.partial = false;
    this.paths = [];
    for (var pathsIdx = 0; pathsIdx != paths.length; pathsIdx++) {
      this.paths.push(new Sherpa.Path(this, paths[pathsIdx]));
    }
  },
  Node: function() {
    this.reset();
  },
  Response: function(path, params) {
    this.path = path
    this.route = path.route;
    this.paramsArray = params;
    this.destination = this.route.destination;
    this.params = {};
    for (var varIdx = 0; varIdx != this.path.variableNames.length; varIdx++) {
      this.params[this.path.variableNames[varIdx]] = this.paramsArray[varIdx];
    }
  }
};

Sherpa.Node.prototype = {
  reset: function() {
    this.linear = [];
    this.lookup = {};
    this.catchall = null;
  },
  dup: function() {
    var newNode = new Sherpa.Node();
    for(var idx = 0; idx != this.linear.length; idx++) {
      newNode.linear.push(this.linear[idx]);
    }
    for(var key in this.lookup) {
      newNode.lookup[key] = this.lookup[key];
    }
    newNode.catchall = this.catchall;
    return newNode;
  },
  addLinear: function(regex, count) {
    var newNode = new Sherpa.Node();
    this.linear.push([regex, count, newNode]);
    return newNode;
  },
  addCatchall: function() {
    if (!this.catchall) {
      this.catchall = new Sherpa.Node();
    }
    return this.catchall;
  },
  addLookup: function(part) {
    if (!this.lookup[part]) {
      this.lookup[part] = new Sherpa.Node();
    }
    return this.lookup[part];
  },
  addRequestNode: function() {
    if (!this.requestNode) {
      this.requestNode = new Sherpa.Node();
      this.requestNode.requestMethod = null;
    }
    return this.requestNode;
  },
  find: function(parts, request, params) {
    if (this.requestNode || this.destination && this.destination.route.partial) {
      var target = this;
      if (target.requestNode) {
        target = target.requestNode.findRequest(request);
      }
      if (target && target.destination && target.destination.route.partial) {
        return new Sherpa.Response(target.destination, params);
      }
    }
    if (parts.length == 0) {
      var target = this;
      if (this.requestNode) {
        target = this.requestNode.findRequest(request);
      }
      return target && target.destination ? new Sherpa.Response(target.destination, params) : undefined;
    } else {
      if (this.linear.length != 0) {
        var wholePath = parts.join('/');
        for (var linearIdx = 0; linearIdx != this.linear.length; linearIdx++) {
          var lin = this.linear[linearIdx];
          var match = lin[0].exec(wholePath);
          if (match) {
            var matchedParams = [];
            if (match[1] === undefined) {
              matchedParams.push(match[0]);
            } else {
              for (var matchIdx = 1; matchIdx <= lin[1] + 1; matchIdx++) {
                matchedParams.push(match[matchIdx]);
              }
            }

            var newParams = params.concat(matchedParams);
            matchedIndex = match.shift().length;
            var resplitParts = wholePath.substring(matchedIndex).split('/');
            if (resplitParts.length == 1 && resplitParts[0] == '') resplitParts.shift();
            var potentialMatch = lin[2].find(resplitParts, request, newParams);
            if (potentialMatch) return potentialMatch;
          }
        }
      }
      if (this.lookup[parts[0]]) {
        var potentialMatch = this.lookup[parts[0]].find(parts.slice(1, parts.length), request, params);
        if (potentialMatch) return potentialMatch;
      }
      if (this.catchall) {
        var part = parts.shift();
        params.push(part);
        return this.catchall.find(parts, request, params);
      }
    }
    return undefined;
  },
  findRequest: function(request) {
    if (this.requestMethod) {
      if (this.linear.length != 0 && request[this.requestMethod]) {
        for (var linearIdx = 0; linearIdx != this.linear.length; linearIdx++) {
          var lin = this.linear[linearIdx];
          var match = lin[0].exec(request[this.requestMethod]);
          if (match) {
            matchedIndex = match.shift().length;
            var potentialMatch = lin[2].findRequest(request);
            if (potentialMatch) return potentialMatch;
          }
        }
      }
      if (request[this.requestMethod] && this.lookup[request[this.requestMethod]]) {
        var potentialMatch = this.lookup[request[this.requestMethod]].findRequest(request);
        if (potentialMatch) {
          return potentialMatch;
        }
      }
      if (this.catchall) {
        return this.catchall.findRequest(request);
      }
    } else if (this.destination) {
      return this;
    } else {
      return undefined;
    }
  },
  transplantValue: function() {
    if (this.destination && this.requestNode) {
      var targetNode = this.requestNode;
      while (targetNode.requestMethod) {
        targetNode = (targetNode.addCatchall());
      }
      targetNode.destination = this.destination;
      this.destination = undefined;
    }
  },
  compileRequestConditions: function(router, requestConditions) {
    var currentNodes = [this];
    var requestMethods = router.requestKeys;
    for (var requestMethodIdx in requestMethods) {
      var method = requestMethods[requestMethodIdx];
      if (requestConditions[method]) {// so, the request method we care about it ..
        if (currentNodes.length == 1 && currentNodes[0] === this) {
          currentNodes = [this.addRequestNode()];
        }

        for (var currentNodeIndex = 0; currentNodeIndex != currentNodes.length; currentNodeIndex++) {
          var currentNode = currentNodes[currentNodeIndex];
          if (!currentNode.requestMethod) {
            currentNode.requestMethod = method
          }

          var masterPosition = requestMethods.indexOf(method);
          var currentPosition = requestMethods.indexOf(currentNode.requestMethod);

          if (masterPosition == currentPosition) {
            if (requestConditions[method].compile) {
              currentNodes[currentNodeIndex] = currentNodes[currentNodeIndex].addLinear(requestConditions[method], 0);
            } else {
              currentNodes[currentNodeIndex] = currentNodes[currentNodeIndex].addLookup(requestConditions[method]);
            }
          } else if (masterPosition < currentPosition) {
            currentNodes[currentNodeIndex] = currentNodes[currentNodeIndex].addCatchall();
          } else {
            var nextNode = currentNode.dup();
            currentNode.reset();
            currentNode.requestMethod = method;
            currentNode.catchall = nextNode;
            currentNodeIndex--;
          }
        }
      } else {
        for (var currentNodeIndex = 0; currentNodeIndex != currentNodes.length; currentNodeIndex++) {
          var node = currentNodes[currentNodeIndex];
          if (!node.requestMethod && node.requestNode) {
            node = node.requestNode;
          }
          if (node.requestMethod) {
            currentNodes[currentNodeIndex] = node.addCatchall();
            currentNodes[currentNodeIndex].requestMethod = null;
          }
        }
      }
    }
    this.transplantValue();
    return currentNodes;
  }
};

Sherpa.Router.prototype = {
  generate: function(name, params) {
    return this.routes[name].generate(params);
  },
  add: function(uri, options) {
    var route = new Sherpa.Route(this, uri);
    if (options) route.withOptions(options);
    return route;
  },
  recognize: function(path, request) {
    if (path.substring(0,1) == '/') path = path.substring(1);
    return this.root.find(path == '' ? [] : path.split(/\//), request, []);
  }
};

Sherpa.Route.prototype = {
  withOptions: function(options) {
    if (options['conditions']) {
      this.condition(options['conditions']);
    }
    if (options['matchesWith']) {
      this.matchesWith(options['matchesWith']);
    }
    if (options['matchPartially']) {
      this.matchPartially(options['matchPartially']);
    }
    if (options['name']) {
      this.matchPartially(options['name']);
    }
    return this;
  },
  name: function(routeName) {
    this.router.routes[routeName] = this;
    return this;
  },
  matchPartially: function(partial) {
    this.partial = (partial === undefined || partial === true);
    return this;
  },
  matchesWith: function(matches) {
    for (var matchesKey in matches) {
      this.matchingConditions[matchesKey] = matches[matchesKey];
    }
    return this;
  },
  compile: function() {
    for(var pathIdx = 0; pathIdx != this.paths.length; pathIdx++) {
      this.paths[pathIdx].compile();
      for (var variableIdx = 0; variableIdx != this.paths[pathIdx].variableNames.length; variableIdx++) {
        if (this.variableNames.indexOf(this.paths[pathIdx].variableNames[variableIdx]) == -1) this.variableNames.push(this.paths[pathIdx].variableNames[variableIdx]);
      }
    }
  },
  to: function(destination) {
    this.compile();
    this.destination = destination;
    return this;
  },
  condition: function(conditions) {
    for (var conditionKey in conditions) {
      this.requestConditions[conditionKey] = conditions[conditionKey];
    }
    return this;
  },
  generate: function(params) {
    var path = undefined;
    if (params == undefined || this.paths.length == 1) {
      path = this.paths[0].generate(params);
    } else {
      for(var pathIdx = this.paths.length - 1; pathIdx >= 0; pathIdx--) {
        path = this.paths[pathIdx].generate(params);
        if (path) break;
      }
    }

    if (path) {
      path = encodeURI(path);
      var query = '';
      for (var key in params) {
        query += (query == '' ? '?' : '&') + encodeURIComponent(key).replace(/%20/g, '+') + '=' + encodeURIComponent(params[key]).replace(/%20/g, '+');
      }
      return path + query;
    } else {
      return undefined
    }
  }
};

Sherpa.Path.prototype = {
  pathSplit: function(path) {
    var splitParts = [];
    var parts = path.split('/');
    if (parts[0] == '') parts.shift();

    for(var i = 0; i != parts.length; i++) {
      splitParts.push("/");
      splitParts.push("");
      partChars = parts[i].split('');

      var inVariable = false;

      for (var j = 0; j != partChars.length; j++) {
        if (inVariable) {
          var code = partChars[j].charCodeAt(0);
          if ((code >= 48 && code <= 57) || (code >= 65 && code <= 90) || (code >= 97 && code <= 122) || code == 95) {
            splitParts[splitParts.length - 1] += partChars[j];
          } else {
            inVariable = false;
            splitParts.push(partChars[j]);
          }
        } else if (partChars[j] == ':') {
          inVariable = true;
          if (splitParts[splitParts.length - 1] == '') {
            splitParts[splitParts.length - 1] += ":";
          } else {
            splitParts.push(":");
          }
        } else {
          splitParts[splitParts.length - 1] += partChars[j];
        }
      }
    }
    return splitParts;
  },
  generate: function(params) {
    for(var varIdx = 0; varIdx != this.variableNames.length; varIdx++) {
      if (!params[this.variableNames[varIdx]]) return undefined;
    }
    for(var varIdx = 0; varIdx != this.variableNames.length; varIdx++) {
      if (this.route.matchingConditions[this.variableNames[varIdx]]) {
        if (this.route.matchingConditions[this.variableNames[varIdx]].exec(params[this.variableNames[varIdx]].toString()) != params[this.variableNames[varIdx]].toString()) {
          return undefined;
        }
      }
    }
    var path = eval(this.compiledUri);
    for(var varIdx = 0; varIdx != this.variableNames.length; varIdx++) {
      delete params[this.variableNames[varIdx]];
    }
    return path;
  },
  compile: function() {
    this.variableNames = [];
    var currentNode = this.route.router.root;
    for(var groupIdx = 0; groupIdx != this.groups.length; groupIdx++) {
      var group = this.groups[groupIdx];
      if (group.length > 1) {
        var pattern = '^';
        for (var partIndex = 0; partIndex != group.length; partIndex++) {
          var part = group[partIndex];
          var captureCount = 0
          if (part.substring(0,1) == ':') {
            var variableName = part.substring(1);
            this.variableNames.push(variableName);
            pattern += this.route.matchingConditions[variableName] ? this.route.matchingConditions[variableName].toString() : '(.*?)'
            captureCount += 1
          } else {
            pattern += RegExp.escape(part);
          }
        }
        currentNode = currentNode.addLinear(new RegExp(pattern), captureCount);
      } else if (group.length == 1) {
        var part = group[0];
        if (part.substring(0,1) == ':') {
          var variableName = part.substring(1);
          this.variableNames.push(variableName);
          if (this.route.matchingConditions[variableName]) {
            currentNode = currentNode.addLinear(this.route.matchingConditions[variableName], 1);
          } else {
            currentNode = currentNode.addCatchall();
          }
        } else {
          currentNode = currentNode.addLookup(part);
        }
      }
    }
    var nodes = currentNode.compileRequestConditions(this.route.router, this.route.requestConditions);
    for (var nodeIdx = 0; nodeIdx != nodes.length; nodeIdx++) {
      nodes[nodeIdx].destination = this;
    }
  }
};
