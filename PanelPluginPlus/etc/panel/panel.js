var selectedid="";
var selectedclass="";
var selectmode=false;
var selectattr="selectBackgroundColor";
var changeElement="selectId";

function select(itemid,classid) {
  selectedid=itemid;
  selectedclass=classid;
  selectmode=true;
  openColorMap();
}
function selectStyleColor(valueid) {
  selectattr=valueid;
  showId("colorInput");
  hideId("styleInput");
  hideId("customInput");
  removeAllStyleMarks();
  addClassId(valueid,"mark");
}
function selectStyle(valueid) {
  selectattr=valueid;
  if(selectattr=='selectCustom') {
    hideId("colorInput");
    hideId("styleInput");
    showId("customInput");
  } else {
    hideId("colorInput");
    showId("styleInput");
    setValueId("styleInput",getStyleId(selectedid,valueid));
    hideId("customInput");
  }
  removeAllStyleMarks();
  addClassId(valueid,"mark");
}
function removeAllStyleMarks() {
  removeClassId("selectTextColor","mark");
  removeClassId("selectBackgroundColor","mark");
  removeClassId("selectBorderColor","mark");
  removeClassId("selectFont","mark");
  removeClassId("selectFontSize","mark");
  removeClassId("selectBorder","mark");
  removeClassId("selectCorners","mark");
  removeClassId("selectPadding","mark");
  removeClassId("selectMargin","mark");
  removeClassId("selectMinWidth","mark");
  removeClassId("selectMinHeight","mark");
  removeClassId("selectScroll","mark");
  removeClassId("selectCustom","mark");
}
function selectChangeElement(changeElementId) {
  changeElement=changeElementId;
  showId("styleSelectorColor");
  showId("styleSelector");
  removeAllElementMarks();
  addClassId(changeElementId,"mark");
}
function removeAllElementMarks() {
  removeClassId("selectId","mark");
  removeClassId("selectClass","mark");
  removeClassId("selectBody","mark");
}
function alterColor(hex) {
  if (selectmode) {
    if (changeElement == 'selectId') {
      var item = document.getElementById(selectedid);
      changeColor(item,selectattr,hex);
    } else if (changeElement == 'selectClass') {
      var panels = document.getElementsByClassName(selectedclass);
      for (i = 0; i < panels.length; i++) {
        var panel = panels[i];
        changeColor(panel,selectattr,hex);
      }
    } else if (changeElement == 'selectBody') {
      var item = document.getElementsByTagName('body')[0];
      changeColor(item,selectattr,hex);
    }
  }
}
function changeColor(ele,selectAttId,hex) {
  if (selectAttId == "selectBackgroundColor") {
    ele.style.backgroundColor=hex;
  } else if (selectAttId == "selectTextColor") {
    ele.style.color=hex;
  } else if (selectAttId == "selectBorderColor") {
    ele.style.borderColor=hex;
  }
}
function alterStyle(value) {
  if (selectmode) {
    if (changeElement == 'selectId') {
      var item = document.getElementById(selectedid);
      changeStyle(item,selectattr,value);
    } else if (changeElement == 'selectClass') {
      var panels = document.getElementsByClassName(selectedclass);
      for (i = 0; i < panels.length; i++) {
        var panel = panels[i];
        changeStyle(panel,selectattr,value);
      }
    } else if (changeElement == 'selectBody') {
      var item = document.getElementsByTagName('body')[0];
      changeStyle(item,selectattr,value);
    }
  }
}
function changeStyle(ele,selectAttId,value) {
  if (selectAttId == "selectFont") {
    ele.style.font=value;
  } else if (selectAttId == "selectFontSize") {
    ele.style.fontSize=value;
  } else if (selectAttId == "selectBorder") {
    ele.style.border=value;
  } else if (selectAttId == "selectCorners") {
    ele.style.borderRadius=value;
  } else if (selectAttId == "selectPadding") {
    ele.style.padding=value;
  } else if (selectAttId == "selectMargin") {
    ele.style.margin=value;
  } else if (selectAttId == "selectMinWidth") {
    ele.style.minWidth=value;
  } else if (selectAttId == "selectMinHeight") {
    ele.style.minHeight=value;
  } else if (selectAttId == "selectScroll") {
    ele.style.overflow=value;
  } else if (selectAttId == "selectCustom") {
    ele.style.custom=value;
  }
}
function getStyleId(eleId,selectAttId) {
  ele=document.getElementById(eleId);
  return getStyle(ele,selectAttId);
}
function getStyle(ele,selectAttId) {
  if (selectAttId == "selectFont") {
    return ele.style.font;value;
  } else if (selectAttId == "selectFontSize") {
    return ele.style.fontSize;
  } else if (selectAttId == "selectBorder") {
    return ele.style.border;
  } else if (selectAttId == "selectCorners") {
    return ele.style.borderRadius;
  } else if (selectAttId == "selectPadding") {
    return ele.style.padding;
  } else if (selectAttId == "selectMargin") {
    return ele.style.margin;
  } else if (selectAttId == "selectMinWidth") {
    return ele.style.minWidth;
  } else if (selectAttId == "selectMinHeight") {
    return ele.style.minHeight;
  } else if (selectAttId == "selectScroll") {
    return ele.style.overflow;
  } else if (selectAttId == "selectCustom") {
    return ele.style.custom;
  }
}

function openColorMap() {
  showId("colorSelectDiv");
}
function closeColorMap() {
  hideId("colorSelectDiv");
  selectattr="selectBackground";
  changeElement="selectId";
  removeAllElementMarks();
  removeAllStyleMarks();
}
function toggleEditMode(itemid,classid) {
  var panelid = 'panel-'+itemid;
  var panel = document.getElementById(panelid);
  var editEle = document.getElementById(itemid);
  if (hasClass(panel,'selected')) {
    disableClick(itemid,classid);
    editEle.innerHTML="Edit";
    closeColorMap();
    hideId("styleSelectorColor");
    hideId("styleSelector");
    hideId("colorInput");
    hideId("styleInput");
    hideId("customInput");
    removeClass(panel,'selected');
  } else {
    addClass(panel,'selected');
    select("content-"+itemid,"content-"+classid)
    enableClick(itemid,classid);
    editEle.innerHTML="Close";
    openColorMap();
  }
}
function toggleDisplayId(eleId) {
  ele=document.getElementById(eleId);
  toggleDisplay(ele);
}
function toggleDisplay(ele) {
  if (ele) {
    if (ele.style.display=="block") {
      hide(ele);
    } else {
      show(ele);
    }
  }
}
function showId(eleId) {
  ele=document.getElementById(eleId);
  show(ele);
}
function hideId(eleId) {
  ele=document.getElementById(eleId);
  hide(ele);
}
function show(ele) {
  if (ele) {
    ele.style.display="block";
  }
}
function hide(ele) {
  if (ele) {
    ele.style.display="none";
  }
}
var clickFunction = function() {
  select(this.id,this.className.substring(7));
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
function setValueId(eleId,value) {
  var ele = document.getElementById(eleId);
  return setValue(ele,value);
}
function setValue(ele,value) {
  return ele.value = value;
}
function hasClassId(eleId,cls) {
  var ele = document.getElementById(eleId);
  return hasClass(ele,cls);
}
function hasClass(ele,cls) {
  return ele.className.match(new RegExp('(\\s|^)'+cls+'(\\s|$)'));
}

function addClassId(eleId,cls) {
  var ele = document.getElementById(eleId);
  addClass(ele,cls);
}
function addClass(ele,cls) {
  if (!hasClass(ele,cls)) ele.className += " "+cls;
}

function removeClassId(eleId,cls) {
  var ele = document.getElementById(eleId);
  removeClass(ele,cls);
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
