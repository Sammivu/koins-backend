//package com.middleware.config;
//
//import com.middleware.service.impl.LoanServiceImpl;
//import com.middleware.service.impl.WalletServiceImpl;
//import lombok.RequiredArgsConstructor;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class WalletHashRepairRunner implements CommandLineRunner {
//
//    private final WalletServiceImpl walletService;
//    private final LoanServiceImpl loanService;
//
//    @Override
//    public void run(String... args) {
//
//        walletService.repairWalletHashes();
//    }
//
//
//    @Override
//    public void run(String... args) {
//
//        loanService.repairLoanHashes();
//    }
//}