#!/bin/bash

# VARIABLES ====================================================================

# Colors
BLACK='\e[0;30m'
BLUE='\e[0;34m'
GREEN='\e[0;32m'
CYAN='\e[0;36m'
RED='\e[0;31m'
PURPLE='\e[0;35m'
BROWN='\e[0;33m'
LIGHTGRAY='\e[0;37m'
DARKGRAY='\e[1;30m'
LIGHTBLUE='\e[1;34m'
LIGHTGREEN='\e[1;32m'
LIGHTCYAN='\e[1;36m'
LIGHTRED='\e[1;31m'
LIGHTPURPLE='\e[1;35m'
YELLOW='\e[1;33m'
WHITE='\e[1;37m'
NC='\e[0m'
BOLD='\e[1m'


pulseaudio="true" # Change to "true" if you use Pulse
frame_rate="30"
video_bitrate="512k" # For two pass
webm_video_bitrate="256k" # For webm
audio_bitrate="160k" # in kilobytes
audio_freq="44100"
crf="25" # For one pass. The higher, the smaller but the crappier
webm_crf="8" # For one pass
preset_lossless="faster" # ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow, placebo
preset="faster" # for encoding
output_destination="${HOME}"
dependencies=( x264 ffmpeg libvorbis libvpx xwininfo xrectsel )
temp_dir="$(mktemp -d -t ffmpeg.XXXXX)"
gif_palette="palette.png"
default_encoding=webm
default_audio=n
default_filename=default_name
default_encode=y
default_pass=1
default_window=frame

create_config() {
echo "pulseaudio=\"$pulseaudio\"
frame_rate=\"$frame_rate\"
video_bitrate=\"$video_bitrate\" # For two pass
webm_video_bitrate=\"$webm_video_bitrate\" # For webm
audio_bitrate=\"$audio_bitrate\" # in kilobytes
audio_freq=\"$audio_freq\"
crf=\"$crf\" # For one pass. The higher, the smaller but the crappier
webm_crf=\"$webm_crf\" # For one pass
preset_lossless=\"$preset_lossless\" # ultrafast, superfast, veryfast, faster, fast, medium, slow, slower, veryslow, placebo
preset=\"$preset\" # for encoding

dependencies=( ${dependencies[@]} )
temp_dir=\"$temp_dir\"
gif_palette=\"$gif_palette\"

# Set the output destination
output_destination=\"$output_destination\"

# File storing last command (in output_destination)
memo_file=$memo_file

# Set default output format [x264|mp4|webm|gif]
default_encoding=$default_encoding

# Set audio preferences [y|n]
default_audio=$default_audio

# Set the default output filename without extension
default_filename=$default_filename

# Set if you want to encode directly after recording [y|n]
default_encode=$default_encode

# Set the number of passes [1|2]
default_pass=$default_pass

# Set the type of window you want to record [frame|rectangle]
default_window=$default_window

# Set if you want to keep raw file or not
default_raw=$default_raw

# Set default countdown setting [true|false]
default_countdown=$default_countdown" > $HOME/.sc_config
}

# Load an optional config file
if [ ! -f $HOME/.sc_config ]; then
	create_config
fi

source $HOME/.sc_config

if [ ! -f $output_destination/$memo_file ]; then
   touch $output_destination/$memo_file
fi

usage(){
    ## Print usage of the script and exit
    printf "Usage: screencast [-a <I|B|H|N>] [-c] [-e <x|m|w|g>] [--filename=<filename>] [-n] [-o [n]] [-p <1|2>] [-q] [--raw] [-r] [-w <F|R>]
 \t-a --audio: Set audio input - [I]nternal, [B]uilt-in, [H]eadset, [N]o audio
 \t-c --countdown: Remove countdown
 \t-d --default: Set options by default (webm, no audio, frame, default filename)
 \t-e --encoding: Set encoding type - [x]264, [m]p4, [w]ebm, [g]if
 \t-f --filename: Set video filename
 \t--new-config: Create a new .sc_config file
 \t-n --now: Encode without asking
 \t-p --pass: Set number of passes (1/2)
 \t-q --quiet: Quiet - silence ffmpeg
 \t--raw: Keep raw lossless file
 \t-r --repeat: Repeat last command
 \t-w --window: Window to record - [F]rame, [R]ectangle\n"

    exit 0
}

script_options=$(getopt -o cdhnqra:e:f:p::w: --long audio:,countdown,default,encoding:,filename:,help,pass::,quiet,new-config,now,repeat,window,raw -- "$@")

# If foreign option entered, exit
[ $? -eq 0 ] || {
    usage
    exit 1
}

eval set -- "$script_options"

while true; do
    case "$1" in
	-d|--default)
	    audioQ=$default_audio
	    encoding=$default_encoding
	    file=$default_filename
	    echo "Filename: $default_filename" > $output_destination/$memo_file
	    encode=$default_encode
	    pass=$default_pass
	    screen_selection=$default_window
	    countdown=$default_countdown
	    raw=$default_raw
	    shift;;
	-a|--audio) # Audio options
	    case "$2" in
		*)
		    if [[ $2 == [nN]* ]]; then
			audioQ=No
		    elif [[ $2 == [iIBbHh]* ]]; then
			     audioQ=Yes
			     audioA=$2
		    else
	    		echo "-a: available options [i]nternal|[b]uilt-in|[h]eadset|[n]o"
	    		exit 1
			 fi
		    echo OK
		    shift 2
	    esac;;
	-c|--countdown)
	    countdown=false
	    shift
	    ;;
	-e|--encoding) # Encoding type
	    case "$2" in
		*)
		    if [[ $2 == [WwXxGgMm]* ]]; then
			encoding=$2
		    else
			echo "Invalid encoding option -e <w|x|g|m>. Aborting..."
			exit 1
		    fi
		    shift 2
	    esac;;
	-f|--filename) # Filename
	    case "$2" in
		*)
		    echo "Filename: $2" > $output_destination/$memo_file
		    file=$2
		    shift 2
	    esac;;
	-h|--help) # Help
	    usage
	    ;;
	--new-config) # New config
	    create_config
	    exit 0
	    ;;
	-n|--now) # Encode now
	    encode=y
	    shift
	    ;;
	-p|--pass) # number of passes
	    case "$2" in
		"")
		    pass=1
		    shift 2;;
		1)
		    pass=1
		    shift 2;;
		2)
		    pass=2
		    shift 2;;
		*)
		    echo "Invalid pass option -p [1|2]. Aborting..."
		    shift 2;;
	    esac;;
	-q|--quiet) # silence ffmpeg
	    quiet="-loglevel fatal"
	    shift;;
	-r|--repeat)
	    echo "Repeat mode activated..."
	    file=$(grep Filename: $output_destination/$memo_file | cut -d: -f2-)
	    ext=$(grep Extension: $output_destination/$memo_file | cut -d' ' -f2-)
	    repeat=true
	    countdown=$(grep Countdown: $output_destination/$memo_file | cut -d' ' -f2-)
	    encode=$(grep Encode: $output_destination/$memo_file | cut -d' ' -f2-)
	    raw=$(grep Raw: $output_destination/$memo_file | cut -d' ' -f2-)
	    shift;;
	--raw)
	    raw=true
	    shift;;
	-w|--window) # Window capture
	    case "$2" in
		*)
		    if [[ $2 == [FfRr]* ]]; then
			screen_selection=$2
			if [[ $2 == [Ff]* ]]; then
			    echo "Window capture set to 'frame'..."
			else
			    echo "Window capture set to 'rectangle'..."
			fi
		    else
			echo "Invalid capture option -w. Aborting..."
			exit 1
		    fi
		    shift 2;;
	    esac;;
	--)
	    shift
	    break
	    ;;

	:)
	    echo "Option -$OPTARG requires an argument." >&2
	    exit 1
	    ;;
	\?)
	    usage
	    ;;
    esac
done




#===============================================================================

# FUNCTIONS ====================================================================

check_for_dependencies() {
    ## Check for missing dependencies

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
    ## Move working directory to /tmp
    mkdir -p $temp_dir
    cd $temp_dir
}

set_encoding_type() {
    ## Set the type of video encoding

    # If 'repeat' mode on, skip function
    if [[ $repeat == true ]]; then
	return
    fi

    # If encoding is not yet set, set it
    if [ -z ${encoding+x} ]; then
	echo "Would you like x264, mp4, webm or gif encoding? [x264]"
	read encoding
    fi

    # For gif only
    if [[ "$encoding" == [Gg]* ]]; then
	echo "Which size for the gif, sir? [320]"
	read scale
	if [ -z "$scale" ]; then
	   scale=320
	fi
    fi
}

set_audio_variables() {
    ## Set audio variables (internal, built-in, headset)

    # If 'repeat' mode on, skip function
    if [[ $repeat == true ]]; then
	return
    fi

    # If gif is created, skip function
    if [[ "$encoding" == [Gg]* ]]; then
	return
    fi

    # If audio not already set, ask it
    if [ -z ${audioQ+x} ]; then
	echo "Would you like audio? y/N [N]"
	read audioQ
    fi

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
	    if [ -z ${audioA+x} ]; then
		echo "Where would you like the audio to come from?"
		echo ""
		echo "1) Internal audio"
		echo "2) Built-in microphone"
		echo "3) Headset microphone"
		echo ""
		echo "Enter single digit (Press space when finished) [1]"
		read audioA
		clear
	    fi
            echo "Pavucontrol will now open."
            echo ""
            if [[ $audioA == 2 ]]; then
                echo 'Go to the Recording tab and choose "Internal Audio".'
                AC="2"
            elif [[ $audioA == 3 ]]; then
                echo 'Go to the Recording tab and choose your headset'
                AC="2"
	    else
		echo 'Go to the Recording tab and choose "Monitor of Internal Audio".'
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

	if [[ "$encoding" == [Ww]* ]]; then
            audio_options="-c:a libvorbis -b:a $audio_bitrate -ac $AC"
	    # For mp4
	elif [[ "$encoding" == [Mm]* ]]; then
            audio_options="-c:a libfaac -b:a $audio_bitrate -ac $AC"
	else
	    # For mkv
            audio_options="-c:a libvorbis -b:a $audio_bitrate -ac $AC"
	fi
    else
	audio_options="-an"
    fi
}

set_window_variables() {
    ## Set which part of the screen to record (frame or rectangle)

    # If 'repeat' mode on, exit function
    if [[ $repeat == true ]]; then
	return
    fi

    # If window variable not already set, ask it
    if [ -z ${screen_selection+x} ]; then
	echo "Would you like to record a frame or a custom rectangle? [frame]"
	echo "1) Frame"
	echo "2) Rectangle"
	read screen_selection
    fi

    # Rectangle mode
    if [[ $screen_selection == [2Rr]* ]]; then
	clear
	echo "Draw the rectange you want to record"
	rectangle=$(xrectsel)
	WIN_GEO=$(echo $rectangle | cut -d\+ -f1)
	WIN_POS=$(echo $rectangle | cut -d\+ -f2,3 | tr "+" ",")
    else
	# Frame mode
	clear
	read -n 1 -p "Press any key then click on the window you wish to record"
	INFO=$(xwininfo -frame)

	# Put information into variables
	WIN_GEO=$(echo "$INFO" | grep -e "Height:" -e "Width:" | cut -d\: -f2 | tr "\n" " " | awk '{print $1 "x" $2}')
	WIN_POS=$(echo "$INFO" | grep "upper-left" | head -n 2 | cut -d\: -f2 | tr "\n" " " | awk '{print $1 "," $2}')
    fi
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
    ## Set file name

    # If 'repeat' mode on, get filename from memo file
    if [[ "$repeat" == true ]]; then
	return
    fi
    # If filename not already set, ask it
    if [ -z ${file+x} ]; then
       clear
       echo "Name file: (without extension)"
       read file
       echo "Filename: $file" > $output_destination/$memo_file

    fi
}

countdown() {
    ## Sets the countdown

    # Stores the countdown value, only if not in repeat mode
    if [[ "$repeat" != true ]]; then
	echo "Countdown: $countdown" >> $output_destination/$memo_file
    fi

    # If no countdown, exit function
    if [ $countdown = false ]; then
	return
    fi
    # Require key press to continue
    clear
    read -n 1 -p "Press any key to record"

    # Wait for 5 seconds to prepare for recording
    clear
    printf "Recording will begin in ${RED}5 "
    sleep 1
    printf "4 "
    sleep 1
    printf "3 "
    sleep 1
    printf "2 "
    sleep 1
    printf "1 \033[0m"
    sleep 1
}

record_lossless() {
    ## Set the record_lossless command (from repeat, with audio or without), run it and then store it in memo file

    # If 'repeat' mode is on, get command from memo file
    if [[ "$repeat" == true ]]; then
	record_lossless_command=$(grep Lossless: $output_destination/$memo_file | cut -d: -f2-)
    # With audio
    elif [[ $audioQ == [yY]* ]]; then
	record_lossless_command="ffmpeg $quiet -thread_queue_size 512 -f alsa -ac $AC -ar $audio_freq -i $incoming -f x11grab -framerate $frame_rate -s $WIN_GEO -i ${DISPLAY}.0+$WIN_POS -c:a pcm_s16le -c:v libx264 -qp 0 -preset $preset_lossless -threads 0 lossless.mkv"
    # Without audio
    else
	record_lossless_command="ffmpeg $quiet -f x11grab -framerate $frame_rate -s $WIN_GEO -i ${DISPLAY}.0+$WIN_POS -c:v libx264 -qp 0 -preset $preset_lossless -threads 0 lossless.mkv"
    fi
    # Store command into memo file, except if in repeat mode
    if [[ "$repeat" != true ]]; then
	echo "Lossless: $record_lossless_command" >> $output_destination/$memo_file
    fi
    echo "Recording!"
    # Start recording
    eval $record_lossless_command
}

ask_to_encode() {
    # Ask if you want to encode the video now or wait until later

    # If 'encode' variable is not yet set
    if [ -z ${encode+x} ]; then
	echo "Would you like to encode the video now? Y/n"
	read encode
	echo "Encode: $encode" >> $output_destination/$memo_file
    fi

    # If set to no, simply move the raw file and quit
    if [[ $encode == [nN]* ]]; then
        mv $temp_dir/lossless.mkv $output_destination/${file}_lossless.mkv
        rm -rf $temp_dir
        exit 0
    fi
}

set_encoding_variables() {
    ## Sets variable for encoding, either from repeat mode, with multiple passes, or from webm/mp4/mkv, and then store the command into memo file

    # In 'repeat' mode, this function is skipped
    if [[ $repeat == true ]]; then
	return
    fi

    ## CHOOSING ENCODING TYPE
    # For gif
    if [[ "$encoding" == [Gg]* ]]; then
	video_options="fps=$frame_rate,scale=$scale:-1:flags=lanczos"
	ext="gif"
	crf_options=""
	echo "Extension: $ext" >> $output_destination/$memo_file
	return
    fi
    # If 'pass' variable is not yet set
    if [ -z ${pass+x} ]; then
	clear
	echo "Choose encoding type:"
	echo ""
	echo "1) Single pass"
	echo "2) Two pass"
	echo ""
	echo "Enter single digit (Default: 1)"
	read pass
    fi
    # For webm
    if [[ "$encoding" == [Ww]* ]]; then
        ext="webm"
        video_options="-c:v libvpx -threads 7 -b:v $webm_video_bitrate"
	      crf_options="-crf $webm_crf"
    # For mp4
    elif [[ "$encoding" == [Mm]* ]]; then
        ext="mp4"
        video_options="-c:v libx264 -preset $preset -threads 0"
        crf_options="-crf $crf"
    else
    # For mkv
        ext="mkv"
        video_options="-c:v libx264 -preset $preset -threads 0"
      	crf_options="-crf $crf"
    fi
    # Store extention into memo file
    echo "Extension: $ext" >> $output_destination/$memo_file
}

encode_video() {
    # If repeat option active, get the stored command and run it
    if [[ $repeat == true ]]; then
	encode_command=$(grep Encoding: $output_destination/$memo_file | cut -d: -f2-)
#	echo $encode_command
	eval "$encode_command"
	return
    fi

    ## SETTING ENCODE COMMAND
    # If creating gif
    if [[ "$encoding" == [Gg]* ]]; then
	encode_command="ffmpeg $quiet -v warning -i lossless.mkv -vf \"$video_options,palettegen\" -y $gif_palette && ffmpeg $quiet -v warning -i lossless.mkv -i $gif_palette -lavfi \"$video_options [x]; [x][1:v] paletteuse\" -y $file.$ext"
    # If creating 2 passes video
    elif [[ $pass == 2 ]]; then
        video_options="$video_options -b:v $video_bitrate"
        if [[ $audioQ == [yY]* ]]; then
            encode_command="ffmpeg $quiet -i lossless.mkv -pass 1 $video_options -f rawvideo -an -y /dev/null &&  ffmpeg $quiet -i lossless.mkv -pass 2 $audio_options $video_options $file.$ext"
        else
            encode_command="ffmpeg $quiet -i lossless.mkv -pass 1 $video_options -f rawvideo -an -y /dev/null &&  ffmpeg $quiet -i lossless.mkv -pass 2 -an $video_options $file.$ext"
        fi
    else
	# Standard command
	encode_command="ffmpeg $quiet -i lossless.mkv $audio_options $video_options $crf_options $file.$ext"
    fi
    # Running encode command
    eval $encode_command
    # Storing encode command into memo file
    echo "Encoding: $encode_command" >> $output_destination/$memo_file
}

cleanup() {
    # Remove unnecessary files and folders and exit
    mv $file.$ext $output_destination/
    if [ -z ${raw+x} ]; then
	echo "Would you like to keep the raw video? y/N"
	read raw
	echo "Raw: $raw" >> $output_destination/$memo_file
    fi

    if [[ $raw == [yY]* ]]; then
        mv $temp_dir/lossless.mkv $output_destination/${file}_lossless.${ext}
        rm -rf $temp_dir
    else
        rm -rf $temp_dir
    fi
    cd $HOME

}

#===============================================================================

# COMMANDS =====================================================================


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
