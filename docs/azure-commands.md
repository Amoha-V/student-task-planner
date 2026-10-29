# Azure commands (PowerShell). Check quota BEFORE creating anything.
```
az login
az account show --output table
az provider register --namespace Microsoft.ContainerService
az provider register --namespace Microsoft.ContainerRegistry
az vm list-usage --location centralindia --output table
$RG="rg-task-planner"; $LOC="centralindia"; $ACR="taskplanner$(Get-Random -Maximum 99999)"; $AKS="aks-task-planner"
az group create -n $RG -l $LOC
az acr create -g $RG -n $ACR --sku Basic --admin-enabled true
az aks create -g $RG -n $AKS --node-count 1 --node-vm-size Standard_B2s --generate-ssh-keys --attach-acr $ACR
az aks get-credentials -g $RG -n $AKS --file $HOME\aks-kubeconfig
az acr credential show -n $ACR          # username/password for Jenkins
kubectl get svc student-task-planner --watch   # wait for EXTERNAL-IP
```
Use any VM size your quota allows if Standard_B2s is unavailable. Delete everything afterwards: `az group delete -n $RG --yes`.
