package br.com.sisaws.config;

import br.com.sisaws.certification.*;
import br.com.sisaws.question.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(CertificationRepository certifications, QuestionRepository questions) {
        return args -> {
            if (certifications.count() > 0) return;

            Certification saa = certifications.save(new Certification(
                    "SAA-C03", "AWS Certified Solutions Architect - Associate"
            ));

            save(questions, saa,
                    "Secure Architectures", "IAM", Difficulty.MEDIUM,
                    "Uma aplicação EC2 precisa ler objetos de um bucket S3 sem armazenar access keys no servidor. Qual abordagem é a mais apropriada?",
                    "Use uma IAM Role associada à instância EC2. Isso fornece credenciais temporárias e evita chaves estáticas na máquina.",
                    new String[]{"Criar um IAM User e salvar as chaves no application.yml", "Associar uma IAM Role à instância EC2", "Liberar acesso público ao bucket", "Salvar as credenciais no User Data"}, 1);

            save(questions, saa,
                    "High-Performing Architectures", "S3", Difficulty.EASY,
                    "Uma aplicação precisa armazenar milhões de imagens como objetos com alta durabilidade e acesso via HTTP. Qual serviço se encaixa melhor?",
                    "Amazon S3 é armazenamento de objetos escalável e durável, adequado para imagens, documentos e conteúdo estático.",
                    new String[]{"Amazon EBS", "Amazon S3", "Amazon RDS", "Amazon EC2 Instance Store"}, 1);

            save(questions, saa,
                    "Resilient Architectures", "RDS", Difficulty.MEDIUM,
                    "Uma aplicação usa Amazon RDS e precisa reduzir indisponibilidade causada por falha da infraestrutura da zona principal. Qual recurso atende melhor essa necessidade?",
                    "Uma implantação Multi-AZ mantém uma instância standby sincronizada em outra zona e permite failover gerenciado.",
                    new String[]{"Read Replica apenas", "Multi-AZ", "S3 Transfer Acceleration", "Elastic IP"}, 1);

            save(questions, saa,
                    "Cost-Optimized Architectures", "EC2", Difficulty.MEDIUM,
                    "Um processamento em lote pode ser interrompido e retomado sem impacto e precisa reduzir significativamente o custo de computação. Qual opção de EC2 é mais adequada?",
                    "Spot Instances são indicadas para workloads tolerantes a interrupções e podem oferecer grande economia em relação a On-Demand.",
                    new String[]{"Dedicated Hosts", "On-Demand Instances", "Spot Instances", "Capacity Reservations"}, 2);

            save(questions, saa,
                    "Secure Architectures", "VPC", Difficulty.MEDIUM,
                    "Qual componente funciona como firewall stateful no nível da interface de rede de recursos dentro de uma VPC?",
                    "Security Groups são stateful e operam associados às interfaces de rede. NACLs são stateless e atuam no nível da subnet.",
                    new String[]{"Network ACL", "Security Group", "Route Table", "Internet Gateway"}, 1);

            save(questions, saa,
                    "Resilient Architectures", "SQS", Difficulty.HARD,
                    "Um serviço de pedidos chama diretamente um serviço de faturamento. Picos de tráfego estão causando falhas em cascata. Qual mudança melhora o desacoplamento?",
                    "Colocar uma fila SQS entre os serviços desacopla produtor e consumidor, absorvendo picos e permitindo processamento assíncrono.",
                    new String[]{"Adicionar Elastic IP", "Colocar Amazon SQS entre os serviços", "Substituir RDS por EBS", "Desativar Auto Scaling"}, 1);

            save(questions, saa,
                    "High-Performing Architectures", "CloudFront", Difficulty.MEDIUM,
                    "Usuários globais baixam arquivos de um bucket S3 e reclamam de latência. Os objetos são frequentemente reutilizados. Qual serviço deve ser colocado na frente do S3?",
                    "CloudFront distribui e armazena conteúdo em cache em edge locations, reduzindo latência para usuários globais.",
                    new String[]{"CloudFront", "CloudTrail", "CloudFormation", "AWS Config"}, 0);

            save(questions, saa,
                    "Secure Architectures", "KMS", Difficulty.MEDIUM,
                    "Uma empresa precisa gerenciar chaves de criptografia usadas por serviços AWS e manter controle de permissões e rotação. Qual serviço é apropriado?",
                    "AWS KMS é o serviço gerenciado para criação e controle de chaves criptográficas integradas a diversos serviços AWS.",
                    new String[]{"AWS KMS", "Amazon Inspector", "Route 53", "AWS Artifact"}, 0);

            save(questions, saa,
                    "Resilient Architectures", "Auto Scaling", Difficulty.MEDIUM,
                    "Uma API executada em várias instâncias EC2 precisa aumentar e diminuir a quantidade de instâncias conforme a demanda. Qual combinação é mais apropriada?",
                    "Um Auto Scaling Group ajusta capacidade e um Application Load Balancer distribui requisições entre as instâncias saudáveis.",
                    new String[]{"Auto Scaling Group + Application Load Balancer", "S3 + Glacier", "Route 53 apenas", "DynamoDB Streams + EBS"}, 0);

            save(questions, saa,
                    "High-Performing Architectures", "DynamoDB", Difficulty.HARD,
                    "Uma aplicação serverless exige banco NoSQL totalmente gerenciado com baixa latência em escala e integração nativa com Lambda. Qual opção é adequada?",
                    "DynamoDB é um banco NoSQL serverless e totalmente gerenciado, adequado a workloads de baixa latência em grande escala.",
                    new String[]{"Amazon DynamoDB", "Amazon EFS", "Amazon Redshift", "Amazon OpenSearch apenas para transações"}, 0);

            save(questions, saa,
                    "Cost-Optimized Architectures", "S3", Difficulty.MEDIUM,
                    "Logs devem permanecer acessíveis imediatamente por 30 dias. Depois disso, raramente são consultados e podem ser arquivados por anos. Qual recurso automatiza essa movimentação?",
                    "S3 Lifecycle Rules permitem transicionar objetos entre classes de armazenamento e expirá-los conforme políticas de retenção.",
                    new String[]{"S3 Lifecycle Rules", "EC2 Auto Scaling", "IAM Access Analyzer", "VPC Peering"}, 0);

            save(questions, saa,
                    "Resilient Architectures", "Route 53", Difficulty.EXAM,
                    "Uma aplicação é executada em duas regiões. O requisito é direcionar usuários para a região saudável e remover automaticamente um endpoint indisponível do roteamento DNS. Qual recurso é necessário?",
                    "Route 53 Health Checks combinados com uma política de roteamento adequada permitem evitar endpoints considerados não saudáveis.",
                    new String[]{"Route 53 Health Checks", "S3 Object Lock", "AWS Budgets", "IAM Permission Boundary"}, 0);
        };
    }

    private void save(QuestionRepository repo, Certification cert, String domain, String service,
                      Difficulty difficulty, String prompt, String explanation,
                      String[] options, int correctIndex) {
        Question q = new Question(cert, domain, service, difficulty, prompt, explanation);
        for (int i = 0; i < options.length; i++) q.addOption(options[i], i == correctIndex);
        repo.save(q);
    }
}
