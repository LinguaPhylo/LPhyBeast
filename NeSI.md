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

- Parallel Execution https://docs.nesi.org.nz/Getting_Started/Next_Steps/Parallel_Execution/
- Slurm https://docs.nesi.org.nz/Getting_Started/Cheat_Sheets/Slurm-Reference_Sheet/
