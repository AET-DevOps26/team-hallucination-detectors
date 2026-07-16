# 1. Create a Resource Group
resource "azurerm_resource_group" "rg" {
  name     = "team-project-rg"
  location = "Sweden Central"
}

# 2. Create a Virtual Network and Subnet
resource "azurerm_virtual_network" "vnet" {
  name                = "team-vnet"
  address_space       = ["10.0.0.0/16"]
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
}

resource "azurerm_subnet" "subnet" {
  name                 = "team-subnet"
  resource_group_name  = azurerm_resource_group.rg.name
  virtual_network_name = azurerm_virtual_network.vnet.name
  address_prefixes     = ["10.0.1.0/24"]
}

# 3. Create a Public IP
resource "azurerm_public_ip" "public_ip" {
  name                = "team-public-ip"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name
  allocation_method   = "Static"
  sku                 = "Standard"
}

# 4. Create a Network Security Group for SSH and the public gateway
resource "azurerm_network_security_group" "nsg" {
  name                = "team-nsg"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  security_rule {
    name                       = "SSH"
    priority                   = 1000
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }

  security_rule {
    name                       = "Gateway"
    priority                   = 1010
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_ranges    = ["3000", "3443"]
    source_address_prefix      = "*"
    destination_address_prefix = "*"
  }
}

# 5. Create a Network Interface and link the NSG
resource "azurerm_network_interface" "nic" {
  name                = "team-nic"
  location            = azurerm_resource_group.rg.location
  resource_group_name = azurerm_resource_group.rg.name

  ip_configuration {
    name                          = "team-nic-config"
    subnet_id                     = azurerm_subnet.subnet.id
    private_ip_address_allocation = "Dynamic"
    public_ip_address_id          = azurerm_public_ip.public_ip.id
  }
}

resource "azurerm_network_interface_security_group_association" "nic_nsg_assoc" {
  network_interface_id      = azurerm_network_interface.nic.id
  network_security_group_id = azurerm_network_security_group.nsg.id
}

# 6. Create the Ubuntu 24.04 Virtual Machine
resource "azurerm_linux_virtual_machine" "vm" {
  name                  = "team-server"
  location              = azurerm_resource_group.rg.location
  resource_group_name   = azurerm_resource_group.rg.name
  network_interface_ids = [azurerm_network_interface.nic.id]
  size                  = "Standard_D2s_v3"
  admin_username        = "azureuser"

  # Uses your local public key so you can SSH in
  admin_ssh_key {
    username   = "azureuser"
    public_key = file(pathexpand("~/.ssh/azure_rsa_key.pub")) # Adjust this path if your key is named differently
  }

  os_disk {
    caching              = "ReadWrite"
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "ubuntu-24_04-lts"
    sku       = "server"
    version   = "latest"
  }
}

# 7. Output the new IP Address to the terminal
output "public_ip_address" {
  value = azurerm_linux_virtual_machine.vm.public_ip_address
}
