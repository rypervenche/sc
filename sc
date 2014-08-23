#!/bin/bash

clear
cat << EOF
sc  Copyright (C) 2011  Rypervenche
This program comes with ABSOLUTELY NO WARRANTY; for details visit <http://www.gnu.org/licenses/gpl.html#section15>.
This is free software, and you are welcome to redistribute it under certain conditions; visit <http://www.gnu.org/licenses/gpl.html#terms>.
EOF
echo ""
read -n 1 -p "Press any key to continue"
clear

# Customize your variables here

pulseaudio="true" # Change to "true" if you use Pulse
ext="mkv"
frame_rate="10"
video_bitrate="512k" # in kilobytes, for two pass
audio_bitrate="160k" # in kilobytes
audio_freq="44100"
crf="18" # For one pass
preset="medium" # ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow, placebo
output_destination="$HOME/Desktop"
dependencies=( x264 ffmpeg libvorbis libvpx )

# Check to see if all required packages are installed
command_exists () {
    type -P "$1" &> /dev/null ;
}

for i in "${dependencies[@]}"
do
    command_exists $i
    if [[ $? != 0 ]]; then
        echo "You need to install ${dependencies[i]}. Closing program now..."
    fi
done

clear

# Move working directory to /tmp
mkdir -p /tmp/screencast
cd /tmp/screencast


# Webm or x264 encoding?
echo "Would you like webm or x264 encoding? [x264]"
read encoding


# Ask if audio is necessary
echo "Would you like audio? y/N"
read audioQ

# Choose audio input
if [[ $audioQ == [yY]* ]]; then
    clear
    if [[ $pulseaudio == "true" ]]; then
        # Check to see if pavucontrol is installed
        if ! type -P pavucontrol > /dev/null; then
            echo "Please install pavucontrol"
            exit 1
        fi
        incoming="pulse"
        clear
        echo "Where would you like the audio to come from?"
        echo ""
        echo "1) Internal audio"
        echo "2) Built-in microphone"
        echo "3) Headset microphone"
        echo ""
        echo "Enter single digit (Press space when finished)"
        read audioA
        clear
        echo "Pavucontrol will now open."
        echo ""
        if [[ $audioA == 1 ]]; then
            echo 'Go to the Recording tab and choose "Monitor of Internal Audio".'
            AC="2"
        elif [[ $audioA == 2 ]]; then
            echo 'Go to the Recording tab and choose "Internal Audio".'
            AC="2"
        elif [[ $audioA == 3 ]]; then
            echo 'Go to the Recording tab and choose your headset'
            AC="2"
        fi
        echo ""
        echo 'Close pavucontrol then press "q" in the terminal.'
        echo ""
        echo "Press any key to continue."
        read -n 1 -p ""
        clear
        pavucontrol &
        ffmpeg -f alsa -ac $AC -i $incoming test_audio.ogg
        rm test_audio.ogg
    elif [[ $pulseaudio == "false" ]]; then
        clear
        cat /proc/asound/cards
        echo ""
        echo "Choose audio device to record from (ex. for hw:1,0 enter \"1\")"
        read audiodevice
        incoming="hw:$audiodevice"
        clear
        echo "Choose number of audio channels (usually mono for microphones)"
        read AC
    else
        exit 1
    fi
fi

# Get window information
clear
read -n 1 -p "Press any key then click on the window you wish to record"
INFO=$(xwininfo -frame)

# Put information into variables
WIN_GEO=$(echo "$INFO" | grep -e "Height:" -e "Width:" | cut -d\: -f2 | tr "\n" " " | awk '{print $1 "x" $2}')
WIN_POS=$(echo "$INFO" | grep "upper-left" | head -n 2 | cut -d\: -f2 | tr "\n" " " | awk '{print $1 "," $2}')
first=$(echo "$WIN_GEO" | cut -d \x -f1)
second=$(echo "$WIN_GEO" | cut -d \x -f2)
if (($first%2!=0)) || (($second%2!=0)); then
    if (($first%2!=0)); then
        first=$(($first-1))
    fi
    if (($second%2!=0)); then
        second=$(($second-1))
    fi
    WIN_GEO="$first"x"$second"
fi    


# Name file
clear
echo "Name file: (without extension)"
read file

# Require key press to continue
clear
read -n 1 -p "Press any key to record"

# Wait for 5 seconds to prepare for recording
clear
echo "Recording will begin in 5 seconds"
sleep 1
clear
echo "Recording will begin in 4 seconds"
sleep 1
clear
echo "Recording will begin in 3 seconds"
sleep 1
clear
echo "Recording will begin in 2 seconds"
sleep 1
clear
echo "Recording will begin in 1 second"
sleep 1
#clear

# Record lossless screencast with or without audio
if [[ $audioQ == [yY]* ]]; then
    ffmpeg -f alsa -ac $AC -ar $audio_freq -i $incoming -f x11grab -r $frame_rate -s $WIN_GEO -i :0.0+$WIN_POS -c:a pcm_s16le -c:v libx264 -qp 0 -preset ultrafast -threads 0 lossless.mkv
else
    ffmpeg -f x11grab -r $frame_rate -s $WIN_GEO -i :0.0+$WIN_POS -c:v libx264 -qp 0 -preset ultrafast -threads 0 lossless.mkv
fi

# Ask if you want to encode the video now or wait until later
#clear
echo "Would you like to encode the video now? Y/n"
read encode

if [[ $encode == [nN]* ]]; then
    mv /tmp/screencast/lossless.mkv $output_destination/${file}_lossless.mkv
    rm -rf /tmp/screencast
    exit 0
fi

# Choose encoding type
clear
echo "Choose encoding type:"
echo ""
echo "1) Single pass"
echo "2) Two pass"
echo ""
echo "Enter single digit (Default: 1)"
read pass

if [[ "$encoding" == [Ww]* ]]
then
    ext="webm"
    audio_options="-c:a libvorbis -b:a $audio_bitrate -ac $AC"
    video_options="-c:v libvpx"
else
    ext="mkv"
    audio_options="-c:a libvorbis -b:a $audio_bitrate -ac $AC"
    video_options="-c:v libx264 -preset $preset"
fi

# Encode video
if [[ $pass == 2 ]]; then
    video_options="$video_options -b:v $video_bitrate"
    if [[ $audioQ == [yY]* ]]; then
        ffmpeg -i lossless.mkv -pass 1 $video_options -threads 0 -f rawvideo -an -y /dev/null
        ffmpeg -i lossless.mkv -pass 2 $audio_options $video_options -threads 0 $file.$ext
    else
        ffmpeg -i lossless.mkv -pass 1 $video_options -threads 0 -f rawvideo -an -y /dev/null
        ffmpeg -i lossless.mkv -pass 2 -an $video_options -threads 0 $file.$ext
    fi

else
    if [[ $audioQ == [yY]* ]]; then
        ffmpeg -i lossless.mkv $audio_options $video_options -crf $crf -threads 0 $file.$ext
    else
        ffmpeg -i lossless.mkv -an $video_options -crf $crf -threads 0 $file.$ext
    fi
fi

# Remove unnecessary files and folders and exit
mv $file.$ext $output_destination/
echo "Would you like to keep the raw video? y/N"
read raw

if [[ $raw == [yY]* ]]; then
    mv /tmp/screencast/lossless.mkv $HOME/Desktop/
    rm -rf /tmp/screencast
else
    rm -rf /tmp/screencast
fi
exit 0
