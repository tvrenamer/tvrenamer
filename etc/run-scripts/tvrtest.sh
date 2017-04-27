#!/bin/bash

prefs=~/.tvrenamer/prefs.xml

srcdir=~/Documents/VC/tvrenamer

indir=~/Desktop/Videos/testdir/intest
outdir=~/Desktop/Videos/testdir/outtest
runprog=run-mingw.sh

if [ "`uname`" = "Darwin" ]
then
  indir=~/Movies/intest
  outdir=~/Movies/outtest
  runprog=run-osx.sh
fi

clean=no
testtoo=no
swap=no

if [ "$1" = "-test" ]
then
  testtoo=yes
fi

if [ "${swap}" = "yes" ]
then
  sed 's,destDir,tempname,g' ${prefs} > ~/x
  sed 's,preloadFolder,destDir,g' ~/x > ~/y
  sed 's,tempname,preloadFolder,g' ~/y > ${prefs}
  /bin/rm ~/x ~/y
fi

/bin/rm -rf ${indir}
/bin/rm -rf ${outdir}

decache ()
{
  if [ -n "$1" ]
  then
    cf=~/.tvrenamer/thetvdb/${1}.xml
    echo $cf
    /bin/rm -f $cf
  fi
}

# decache veep
# decache 237831

if [ "${clean}" = "yes" ]
then
  decache bewitched
  decache dilbert
  decache frasier
  decache friends
  decache futurama
  decache glee
  decache mom
  decache seinfeld
  decache transparent
  decache 71528
  decache 94571
  decache 78581
  decache 77811
  decache 79168
  decache 73871
  decache 83610
  decache 266967
  decache 79169
  decache 278334
fi

/bin/mkdir -p ${indir}
echo contents > ${indir}/Bewitched.S05E02.Samantha.Goes.South.for.a.Spell.avi
echo contents > ${indir}/Dilbert.S01E09.The.Knack.avi
echo contents > ${indir}/Frasier.S10E01.The.Ring.Cycle.mp4
echo contents > ${indir}/Frasier.S10E01.dvdrip.avi
echo contents > ${indir}/Frasier.S10E02.dvdrip.avi
echo contents > ${indir}/Frasier.S10E03.dvdrip.avi
echo contents > ${indir}/Frasier.S10E05.dvdrip.avi
echo contents > ${indir}/Frasier.S10E08.dvdrip.avi
echo contents > ${indir}/Frasier.S10E09.dvdrip.avi
echo contents > ${indir}/Frasier.S10E10.dvdrip.avi
echo contents > ${indir}/Frasier.S10E11.dvdrip.avi
echo contents > ${indir}/Frasier.S10E12.dvdrip.avi
echo contents > ${indir}/Friends.S09E01.The.One.Where.No.One.Proposes.avi
echo contents > ${indir}/Glee.S06E07.Transitioning.m4v
echo contents > ${indir}/Mom.S03E11.Cinderella.and.a.Drunk.MacGyver.mp4
echo contents > ${indir}/Seinfeld.S08E01.The.Foundation.mkv
echo contents > ${indir}/Transparent.S02E08.Oscillate.mp4
echo contents > ${indir}/Veep.S04E07.Mommy.Meyer.mp4

/bin/mkdir ${indir}/Futurama
echo contents > ${indir}/Futurama/S01E07.My.Three.Suns.avi

# Seem to be having a problem with this show.  To be investigated...
# decache community
# echo contents > ${indir}/Community.S04E11.Basic.Human.Anatomy.mkv

# This tests an alias, which are not implemented in this branch
# echo contents > ${indir}/BSG.S04E20.Daybreak.Pt.2.HD.m4v

# /bin/mkdir ${indir}/JohnLarroquetteShow
# /bin/mkdir "${indir}/JohnLarroquetteShow/Season 3"
# echo contents > "${indir}/JohnLarroquetteShow/Season 3/3x18 - The Dance.avi"

# echo contents > "${indir}/Beavis.and.Butt-head.S03E11.x264-FLEET.avi"
# echo contents > ${indir}/MythBusters.2007x14.mpg

/bin/mkdir -p ${outdir}
/bin/mkdir ${outdir}/Veep
echo contents > ${outdir}/Veep/Veep.S04E07.Mommy.Meyer.2015.05.24.mp4

cd ${srcdir}

if [ "${testtoo}" = "yes" ]
then
  ant test&
fi

./etc/run-scripts/${runprog} -build || exit 1

echo '** outdir'
/bin/ls -R ${outdir}

echo '** outdir/Veep'
/bin/ls ${outdir}/Veep

echo
echo '** indir'
/bin/ls -R ${indir}
