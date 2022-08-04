#!/bin/bash

# cd ~/WorkSpace/linguaPhylo.github.io/tutorials/runs/h5n1
# run BEAST
/Applications/BEAST\ 2.6.7/bin/beast -beagle_SSE -overwrite h5n1.xml > out.txt

/Applications/BEAST\ 2.6.7/bin/loganalyser h5n1.log > h5n1.log.txt

# run TreeAnnotator
/Applications/BEAST\ 2.6.7/bin/treeannotator -heights mean -burnin 10 h5n1_with_trait.trees h5n1_with_trait.tree

# run StateTransitionCounter
/Applications/BEAST\ 2.6.7/bin/applauncher StateTransitionCounter -burnin 10 -in h5n1_with_trait.trees -tag location -out stc.out

# plot
Rscript ~/WorkSpace/linguaPhylo.github.io/tutorials/discrete-phylogeography/plotTransitionsHunan.R

