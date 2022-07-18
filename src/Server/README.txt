Para executar o projeto é necessário que estejam na mesma pasta:

- user.txt com os dados dos utilizadores com linhas na forma:
	 [userID];[password];[ccNumber];[nome];[CCValidade];[morada];[departamento];[telefone];[diretoriaAtual]

- config.txt com os portos a serem utilizados [primaryPort];[secondaryPort];[heartbeatPort]
- log.txt vazio
- pasta Data com uma pasta para cada user previamente inicializada

Para utilizar o secondary server copiar a pasta do primary e executar o ficheiro ucDrive.jar

Os ficheiros enviados contém um exemplo de configurações, a título exemplificativo.

Comandos disponíveis:
- cp [oldpassword] [newpassword] [newpassword] --> mudar a password;
- config [primary/secondary] [host] [port] --> configurar endereços e portos do cliente;
- ls [server/local] --> listar o conteúdo da diretoria;
- cd [server/local] [directory] --> permite mudar a diretoria;
- sh --> visualizar as configurações;