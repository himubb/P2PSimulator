# CNT5106C Computer Networks P2P file-sharing Project

CNT5106C FALL 2020

Team (Group-17): 
Himavanth Boddu, UFID: 3245-1847
Piyush Chamria, UFID: 0442-6046
Arushi Singhvi, UFID: 3948-3899

What the project does?
- Implements a P2P file sharing software similar to BitTorrent in Java. 
- Distributes files with choking and unchoking mechanism between peers. 
- Establishing all operations using reliable protocol TCP.

Steps to run the project:
1. Login to CISE thunder remote server 
2. Go to cnfinal folder -- Run "cd cnfinal" 
    The cnfinal folder contains: 
            - The code required to run the project
            - The individual folders for all peers (in our project, we have taken 9 peers with information stated below; hence, 9 folders here from 1001-1009, two of which have the files to be transferred and the rest do not. If you intend to run the source files different to the ones provided to us, delete these folders
            - Log files
            - The PeerInfo.cfg and Common.cfg files NOT included as instructued
3. Run "javac *.java"
4. Run "java startRemotepeers"

For running our project, we have used the following numbers:
- Number Of Preferred Neighbors = 3
- Unchoking Interval = 5 sec
- Optimistic Unchoking Interval = 10 sec
- File Name = thefile
- File Size = 8830356 bytes
- Piece Size = 65536 bytes

Peer Information:
- Peer IDs: 1001-1009
- Address of CISE remote macines: lin114-0x.cise.ufl.edu (x from 0 to 8, for the 9 remote machines)
- Port number at every remote machine: 6939
- The peers that have the file already: 1001 and 1006

The link to our demo video on onedrive@UF is: https://uflorida-my.sharepoint.com/:v:/g/personal/pchamria_ufl_edu/EaRm7Q2p-zlGqdBb0X85LGoB6Al4MRFuqnkAn6K2eiXulA?e=2YsN2Q
