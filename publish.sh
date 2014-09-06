#!/bin/bash

DSDIR=/opt/digitalspider
PLUGINSDIR=$DSDIR/jspwiki-plugins
WIKIDIR=$CATALINA_HOME/webapps/JSPWiki
WEBLIBDIR=$WIKIDIR/WEB-INF/lib

cp $PLUGINSDIR/MyPlugin/dist/*.jar $WEBLIBDIR/
cp $PLUGINSDIR/TreePlugin/dist/*.jar $WEBLIBDIR/
cp $PLUGINSDIR/HitCountPlugin/dist/*.jar $WEBLIBDIR/
cp $PLUGINSDIR/GoComicsPlugin/dist/*.jar $WEBLIBDIR/
unzip -q -o $PLUGINSDIR/ImageGalleryPlugin/dist/*.zip -d $WIKIDIR/
cp $PLUGINSDIR/ImageGalleryPlugin/dist/*.jar $WEBLIBDIR/
cp $PLUGINSDIR/VideoPlugin/dist/*.jar $WEBLIBDIR/
cp $PLUGINSDIR/JiraPlugin/dist/*.jar $WEBLIBDIR/
unzip -q -o $PLUGINSDIR/JiraPlugin/dist/*.zip -d $WEBLIBDIR/
cp $PLUGINSDIR/PluginListPlugin/dist/*.jar $WEBLIBDIR/

