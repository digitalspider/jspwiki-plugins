var selectedid="";
var selectedclass="";
var selectmode=false;
var selectattr="selectBackground";
var selectalter="selectId";

function select(itemid,classid) {
  selectedid=itemid;
  selectedclass=classid;
  selectmode=true;
  openColorMap();
}
function selectAttribute(attributeid) {
  selectattr=attributeid;
}
function selectAlter(alterid) {
  selectalter=alterid;
}
function clickColor(hex) {
  if (selectmode) {
    if (selectalter == 'selectId') {
      var item = document.getElementById(selectedid);
      changeColor(item,hex);
    } else if (selectalter == 'selectClass') {
      var panels = document.getElementsByClassName(selectedclass);
      for (i = 0; i < panels.length; i++) {
        var panel = panels[i];
        changeColor(panel,hex);
      }
    }
  }
}
function changeColor(ele,hex) {
  if (selectattr == "selectBackground") {
    ele.style.backgroundColor=hex;
  } else if (selectattr == "selectText") {
    ele.style.color=hex;
  }
}
function openColorMap() {
  document.getElementById("colorSelectDiv").style.display="block";
}
function closeColorMap() {
  document.getElementById("colorSelectDiv").style.display="none";
  selectattr="selectBackground";
  selectalter="selectId";
}
function toggleEditMode(itemid,classid) {
  var panelid = 'panel-'+itemid;
  var panel = document.getElementById(panelid);
  if (hasClass(panel,'selected')) {
    removeClass(panel,'selected');
    disableClick(itemid,classid);
  } else {
    addClass(panel,'selected');
    enableClick(itemid,classid);
  }
}
var clickFunction = function(itemid,classid) {
  select(this.id,this.className.substring(7))
}
function enableClick(itemid,classid) {
  var panel = document.getElementById("panel-"+itemid);
  var header = document.getElementById("header-"+itemid);
  var content = document.getElementById("content-"+itemid);
  var footer = document.getElementById("footer-"+itemid);
  header.addEventListener("click", clickFunction, true );
  content.addEventListener("click", clickFunction, true );
  footer.addEventListener("click", clickFunction, true );
  panel.style.cursor="pointer";
}
function disableClick(itemid,classid) {
  var panel = document.getElementById("panel-"+itemid);
  var header = document.getElementById("header-"+itemid);
  var content = document.getElementById("content-"+itemid);
  var footer = document.getElementById("footer-"+itemid);
  header.removeEventListener("click", clickFunction,  true );
  content.removeEventListener("click",  clickFunction, true );
  footer.removeEventListener("click", clickFunction, true );
  panel.style.cursor=null;
}
function hasClass(ele,cls) {
  return ele.className.match(new RegExp('(\\s|^)'+cls+'(\\s|$)'));
}

function addClass(ele,cls) {
  if (!hasClass(ele,cls)) ele.className += " "+cls;
}

function removeClass(ele,cls) {
  if (hasClass(ele,cls)) {
    var reg = new RegExp('(\\s|^)'+cls+'(\\s|$)');
    ele.className=ele.className.replace(reg,' ');
  }
}

function saveColor(hex,seltop,selleft)
{
var xhttp,c
if (hex==0)
	{
	c=document.getElementById("colorhex").value;
	}
else
	{
	c=hex;
	}
if (c.substr(0,1)=="#")
	{
	c=c.substr(1);
	}
colorhex="#" + c;
colorhex=colorhex.substr(0,10);
document.getElementById("colorhex").value=colorhex;
if (window.XMLHttpRequest)
  {
  xhttp=new XMLHttpRequest();
  }
else
  {
  xhttp=new ActiveXObject("Microsoft.XMLHTTP");
  }
xhttp.open("GET","http_colorshades.asp?colorhex=" + c + "&r=" + Math.random(),false);
xhttp.send("");
document.getElementById("colorshades").innerHTML=xhttp.responseText;
if (seltop>-1 && selleft>-1)
	{
	document.getElementById("selectedColor").style.top=seltop + "px";
	document.getElementById("selectedColor").style.left=selleft + "px";
	document.getElementById("selectedColor").style.visibility="visible";
	}
else
	{
	document.getElementById("divpreview").style.backgroundColor=colorhex;
	document.getElementById("divpreviewtxt").innerHTML=colorhex;
	document.getElementById("selectedColor").style.visibility="hidden";
	}
//refreshleader()
}
