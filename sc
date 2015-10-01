#!/bin/bash

# VARIABLES ====================================================================
pulseaudio="true" # Change to "true" if you use Pulse
frame_rate="30"
video_bitrate="512k" # For two pass
webm_video_bitrate="8k" # For webm
audio_bitrate="160k" # in kilobytes
audio_freq="44100"
crf="18" # For one pass
webm_crf="10" # For one pass
preset="medium" # ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow, placebo
output_destination="$HOME/Desktop"
dependencies=( x264 ffmpeg libvorbis libvpx )
#===============================================================================

# FUNCTIONS ====================================================================
show_license() {
    clear
    cat << EOF
sc Copyright (C) 2011-2014 Rypervenche
This program comes with ABSOLUTELY NO WARRANTY; for details visit <http://www.gnu.org/licenses/gpl.html#section15>.
This is free software, and you are welcome to redistribute it under certain conditions; visit <http://www.gnu.org/licenses/gpl.html#terms>.
EOF
    echo ""
    read -n 1 -p "Press any key to continue"
    clear
}

# Check to see if all required packages are installed
    check_for_dependencies() {
    for i in "${dependencies[@]}"
    do
        type -P "$i" &> /dev/null
    
        if [[ $? != 0 ]]; then
            echo "You need to install ${dependencies[i]}. Closing program now..."
        fi
    done
    clear
}

move_pwd() {
    # Move working directory to /tmp
    mkdir -p /tmp/screencast
    cd /tmp/screencast
}

set_encoding_type() {
    # Webm or x264 encoding?
    echo "Would you like webm or x264 encoding? [x264]"
    read encoding

    if [[ "$encoding" == [Ww]* ]]
    then
        ext="webm"
        audio_options="-c:a libvorbis -b:a $audio_bitrate -ac $AC"
        video_options="-c:v libvpx -threads 7 -b:v $webm_video_bitrate"
	crf_options="-crf $webm_crf"
    else
        ext="mkv"
        audio_options="-c:a libvorbis -b:a $audio_bitrate -ac $AC"
        video_options="-c:v libx264 -preset $preset -threads 0"
	crf_options="-crf $crf"
    fi
}

set_audio_variables() {
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
}

set_window_variables() {
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
}

set_extension_variable() {
    # Name file
    clear
    echo "Name file: (without extension)"
    read file
}

countdown() {
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
}

record_lossless() {
    # Record lossless screencast with or without audio
    if [[ $audioQ == [yY]* ]]; then
        ffmpeg -f alsa -ac $AC -ar $audio_freq -i $incoming -f x11grab -r $frame_rate -s $WIN_GEO -i ${DISPLAY}.0+$WIN_POS -c:a pcm_s16le -c:v libx264 -qp 0 -preset ultrafast -threads 0 lossless.mkv
    else
        ffmpeg -f x11grab -r $frame_rate -s $WIN_GEO -i ${DISPLAY}.0+$WIN_POS -c:v libx264 -qp 0 -preset ultrafast -threads 0 lossless.mkv
    fi
}

ask_to_encode() {
    # Ask if you want to encode the video now or wait until later
    echo "Would you like to encode the video now? Y/n"
    read encode
    
    if [[ $encode == [nN]* ]]; then
        mv /tmp/screencast/lossless.mkv $output_destination/${file}_lossless.mkv
        rm -rf /tmp/screencast
        exit 0
    fi
}

set_encoding_variables() {
    # Choose encoding type
    clear
    echo "Choose encoding type:"
    echo ""
    echo "1) Single pass"
    echo "2) Two pass"
    echo ""
    echo "Enter single digit (Default: 1)"
    read pass
}

encode_video() {
    # Encode video
    if [[ $pass == 2 ]]; then
        video_options="$video_options -b:v $video_bitrate"
        if [[ $audioQ == [yY]* ]]; then
            ffmpeg -i lossless.mkv -pass 1 $video_options -f rawvideo -an -y /dev/null
            ffmpeg -i lossless.mkv -pass 2 $audio_options $video_options $file.$ext
        else
            ffmpeg -i lossless.mkv -pass 1 $video_options -f rawvideo -an -y /dev/null
            ffmpeg -i lossless.mkv -pass 2 -an $video_options $file.$ext
        fi
    
    else
        if [[ $audioQ == [yY]* ]]; then
            ffmpeg -i lossless.mkv $audio_options $video_options $crf_options $file.$ext
        else
            ffmpeg -i lossless.mkv -an $video_options $crf_options $file.$ext
        fi
    fi
}

cleanup() {
    # Remove unnecessary files and folders and exit
    mv $file.$ext $output_destination/
    echo "Would you like to keep the raw video? y/N"
    read raw
    
    cd $OLDPWD
    
    if [[ $raw == [yY]* ]]; then
        mv /tmp/screencast/lossless.mkv $output_destination/
        rm -rf /tmp/screencast
    else
        rm -rf /tmp/screencast
    fi
    
    exit 0
}

#===============================================================================

# COMMANDS =====================================================================
show_license

check_for_dependencies

move_pwd

set_encoding_type

set_audio_variables

set_window_variables

set_extension_variable

countdown

record_lossless

ask_to_encode

set_encoding_variables

encode_video

cleanup
#===============================================================================
