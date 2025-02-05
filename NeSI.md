# NeSI Guide for BEAST2 developers 

Although NeSI has BEAST2 pre-installed on the cluster, you can still install your own version of BEAST2 for package development purposes.

Learn Bash https://docs.nesi.org.nz/Getting_Started/Cheat_Sheets/Bash-Reference_Sheet/ before start:

## 1. Login NeSI

You need to apply a NeSI account, and login to NeSI e.g. Mahuika. https://docs.nesi.org.nz

Login Troubleshooting https://docs.nesi.org.nz/General/FAQs/Login_Troubleshooting/

### Accessing the HPCs

- JupyterHub  https://docs.nesi.org.nz/Scientific_Computing/Interactive_computing_using_Jupyter/Jupyter_on_NeSI/#jupyter-term
- Terminal  https://docs.nesi.org.nz/Scientific_Computing/Terminal_Setup/Standard_Terminal_Setup/

Make sure you can access your NeSI home folder before the next step.

More training: https://docs.nesi.org.nz/Scientific_Computing/Training/Introduction_to_computing_on_the_NeSI_HPC_YouTube_Recordings/

## 2. Setup BEAST 2

You need to install BEAST 2 and packages in your NeSI **home** folder. Here is the instruction:

i. Download the latest __Linux__ x86 version and upload the _.tgz_ file to your NeSI home folder.

https://github.com/CompEvol/beast2/releases/latest

ii. Go to your NeSI home folder, and unzip everything to a subfolder "beast" under your home folder.

```bash
# Check your location
pwd
# Go to your NeSI home folder, if you are in a different path
cd ~
# unzip BEAST under your home folder
tar -xvzf BEAST.v2.7.7.Linux.x86.tgz
# check if it is there
ls
```

Mac could [automatically extract](https://apple.stackexchange.com/questions/260152/why-does-tar-gz-automatically-extract-the-gzip-archive-when-i-download-it-in-sa) 
the `.tgz` file into a `.tar` file. In this case, you can use the command below without `-z`:

```bash
tar -xvf BEAST.v2.7.7.Linux.x86.tar
```

iii. List or install BEAST 2 packages using [Package Manager](https://www.beast2.org/managing-packages/) from command line.

```bash
# more commands on the website
YOUR_BEAST/bin/packagemanager -list
```

The installed packages are stored under the path `~/.beast/2.7/` where `2.7` is the major version of BEAST that you are using.
Use the command `ls -la ~/.beast/2.7/` to see how many packages (subfolders) are installed.

## 3. Run BEAST 2

**Please note:** **never** run any jobs in the terminal. 
You need to [submit jobs](https://docs.nesi.org.nz/Getting_Started/Next_Steps/Submitting_your_first_job/) to the computation nodes using Slurm. 

Your workspace will be located within the subfolder of your project directory, such as `/nesi/nobackup/nesi???/YOUR_NAME`.
Since this folder is shared with other members, please exercise **extreme caution** when using bash commands, especially when deleting files.

Here is an example template to run a BEAST2 XML file, 
where the placeholders in capital letters should be replaced with your specific details, 
and file paths must be adjusted according to your environment settings:

```template
#!/bin/sh
#SBATCH -J PREFIX-FILE		# The job name
#SBATCH -A nesi???		# The account code
#SBATCH --time=72:00:00         # The walltime
#SBATCH --mem=1G 	        # in total
#SBATCH --cpus-per-task=2       # OpenMP Threads
#SBATCH --ntasks=1              # not use MPI
#SBATCH --hint=multithread      # A multithreaded job, also a Shared-Memory Processing (SMP) job
#SBATCH -D ./			# The initial directory
#SBATCH -o FILE_out.txt		# The output file
#SBATCH -e FILE_err.txt		# The error file

# sacct -j JOBID --format="ReqMem,MaxRSS,CPUTime,AveCPU,Elapsed"
#Whenever SLURM mentions CPUs it is referring to logical CPUs (2 logical CPUs = 1 physical CPU)
#Total mem = mem-per-cpu * task / 2
module load beagle-lib/4.0.0-GCC-11.3.0
module load Java/17

# beast 2.7.x 
srun /home/???/beast/bin/beast -beagle_SSE -seed SEED ../FILE
```

After modify the placeholders into your values, save it to a file, such as `test.sl`. Then use Slurm to submit the job:

```bash
sbatch test.sl
```

Check your job:

```bash
squeue --me
```

More Slurm commands: https://docs.nesi.org.nz/Getting_Started/Cheat_Sheets/Slurm-Reference_Sheet/

### Parallel computing

Parallel Execution https://docs.nesi.org.nz/Getting_Started/Next_Steps/Parallel_Execution/

### Batch processing

How to run 100 XMLs at one time? 
You can directly use or modify the following bash script to submit many jobs at one time:

```bash
#!/usr/bin/env bash
DIR=$1
TEMPLATE=mytemplate

# 1. create templates ready to submit
for file in *.xml; do
seed=$(( ( RANDOM % 10000 )  + 1 ))
base=${file##*-} # after last -
stem=${base%.*}
sed "s/FILE/$file/g;s/PREFIX/$stem/g;s/SEED/$seed/g" ./${TEMPLATE} > $DIR/${stem}.sl
echo "save to $DIR/$stem created from $file at seed $seed using template ${TEMPLATE}"
done

# 2. submit all templates
cd $DIR
echo "$PWD"
for tmpfl in *.sl; do
sbatch $tmpfl
#  rm -f $tmpfl
echo "submit job $tmpfl"
sleep 1
done
cd ..
```

This script consists of two parts. 
The first part generates a Slurm template `${stem}.sl` for each XML file and saves it in the directory `$DIR`. 
The second part navigates to that directory and submits all the jobs.

**Please note**: 
- the logger section in each XML has to use different log file names and tree file names, otherwise they will be overwritten each other.
- use `sleep 1` command between each job submission to prevent overwhelming the cluster 
with too many simultaneous requests.

