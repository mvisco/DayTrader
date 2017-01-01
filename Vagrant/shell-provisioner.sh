#!/usr/bin/env bash
sudo apt-get update
sudo apt-get install -y vbox-guest-additions
sudo apt-get install -y gnome-shell  
sudo apt-get install -y ubuntu-gnome-desktop
sudo apt-get install -y openjdk-7-jdk
sudo apt-get install -y maven
sudo apt-get install -y git
wget https://3230d63b5fc54e62148e-c95ac804525aac4b6dba79b00b39d1d3.ssl.cf1.rackcdn.com/Anaconda-2.3.0-Linux-x86_64.sh
sudo su - vagrant << EOF
echo "IN"
whoami
bash Anaconda-2.3.0-Linux-x86_64.sh -b /home/vagrant/anaconda
echo "PATH=$PATH:/home/vagrant/anaconda/bin" >> .bashrc
sudo apt-get install -y eclipse
sudo apt-get install -y python-dev
EOF
echo "OUT"
whoami
