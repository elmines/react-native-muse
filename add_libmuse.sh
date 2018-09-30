#!/bin/bash

#Absolute path to the libmuse's Android libs folder (as of this writing, <libmuse_directory>/android/libs)
LIB_DIR=$1

#Only make symbolic links to the JAR and the ARM folder, rather than copy them
LINK_ONLY=$2

JAR_INSTALL_DIR=`pwd`/android/libs
ARM_INSTALL_DIR=`pwd`/android/src/main/jniLibs

MUSE_JAR=$LIB_DIR/libmuse_android.jar
ARM_DIR=$LIB_DIR/armeabi-v7a

if [ $LINK_ONLY ]; then
	cd $JAR_INSTALL_DIR
	ln -s $MUSE_JAR
	echo "Made link to $MUSE_JAR inside $JAR_INSTALL_DIR"
	cd $ARM_INSTALL_DIR
	ln -s $ARM_DIR
	echo "Made link to $ARM_DIR inside $ARM_INSTALL_DIR"
else
	echo "Copying files. . ."
	cp $MUSE_JAR $JAR_INSTALL_DIR
	echo "Copied $MUSE_JAR to $JAR_INSTALL_DIR"
	cp -r $ARM_DIR $ARM_INSTALL_DIR
	echo "Copied $ARM_DIR to $ARM_INSTALL_DIR"
fi
