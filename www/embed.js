(function() {
  var id = ("showkr" + Math.random()).replace('.', '');
  return document.write('<link rel="stylesheet" href="http://showkr.solovyov.net/namespaced.css"><script src="http://showkr.solovyov.net/showkr.min.js"></script><div class="showkr"><div class="container"><div class="row">  <div id="' + id + '" class="span12"></div></div></div></div><script>  var els = document.getElementsByTagName("script"), el;  for (var i = 0; i < els.length; i++) {    el = els[i];    if (el.src == "http://showkr.solovyov.net/embed.js") { break; } } console.log(el); showkr.main("' + id +'", {path: el.getAttribute("data-path"), "hide-title": el.getAttribute("data-hide-title")});</script>');
})();
