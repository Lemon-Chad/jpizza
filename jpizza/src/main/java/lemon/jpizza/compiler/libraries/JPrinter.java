package lemon.jpizza.compiler.libraries;

import lemon.jpizza.compiler.types.Type;
import lemon.jpizza.compiler.types.Types;;
import lemon.jpizza.compiler.types.objects.VecType;
import lemon.jpizza.compiler.values.Value;
import lemon.jpizza.compiler.values.functions.NativeResult;
import lemon.jpizza.compiler.vm.JPExtension;
import lemon.jpizza.compiler.vm.VM;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class JPrinter extends JPExtension {
    private static final Map<Integer, PrinterJob> jobs = new HashMap<>();
    private static int innovation = 0;

    public JPrinter(VM vm) {
        super(vm);
    }

    @Override
    public String name() {
        return "jprinter";
    }

    @Override
    public void setup() {
        func("createJob", (args) -> {
            PrinterJob job = PrinterJob.getPrinterJob();
            int id = innovation++;
            jobs.put(id, job);
            return NativeResult.Ok(new Value(id));
        }, Types.INT);

        printFunction("printDialog", (args, job) -> NativeResult.Ok(new Value(job.printDialog())), Types.BOOL);

        printFunction("setTargetImage", (args, job) -> {
            String path = args[0].toString();
            BufferedImage image;
            try {
                image = ImageIO.read(new File(path));
            } catch (Exception e) {
                return NativeResult.Err("PrinterError", e.getMessage());
            }
            job.setPrintable((graphics, pageFormat, pageIndex) -> {
                if (pageIndex > 0) {
                    return Printable.NO_SUCH_PAGE;
                }
                graphics.translate((int) pageFormat.getImageableX(), (int) pageFormat.getImageableY());
                graphics.drawImage(image, 0, 0, (int) pageFormat.getImageableWidth(), (int) pageFormat.getImageableHeight(), null);
                return Printable.PAGE_EXISTS;
            });
            return NativeResult.Ok();
        }, Types.VOID, Types.STRING);

        printFunction("setTargetPrinter", (args, job) -> {
            String printer = args[0].toString();
            PrintService[] jobs = PrinterJob.lookupPrintServices();
            for (PrintService j : jobs) {
                if (j.getName().equals(printer)) {
                    job.setPrintService(j);
                    return NativeResult.Ok();
                }
            }
            return NativeResult.Err("PrinterError", "Printer not found");
        }, Types.VOID, Types.STRING);

        printFunction("setPageCount", (args, job) -> {
            int count = args[0].asNumber().intValue();
            job.setPageable(new Pageable() {
                @Override
                public int getNumberOfPages() {
                    return count;
                }

                @Override
                public PageFormat getPageFormat(int pageIndex) {
                    return new PageFormat();
                }

                @Override
                public Printable getPrintable(int pageIndex) {
                    return (graphics, pageFormat, i) -> Printable.PAGE_EXISTS;
                }
            });
            return NativeResult.Ok();
        }, Types.VOID, Types.INT);

        printFunction("setCopies", (args, job) -> {
            int count = args[0].asNumber().intValue();
            job.setCopies(count);
            return NativeResult.Ok();
        }, Types.VOID, Types.INT);

        printFunction("print", (args, job) -> {
            job.print();
            return NativeResult.Ok();
        }, Types.VOID);

        func("availablePrinters", (args) -> {
            PrintService[] jobs = PrinterJob.lookupPrintServices();
            Value[] printers = new Value[jobs.length];
            for (int i = 0; i < printers.length; i++) {
                printers[i] = new Value(jobs[i].getName());
            }
            return NativeResult.Ok(new Value(Arrays.asList(printers)));
        }, new VecType(Types.STRING));
    }

    private interface PrintFunction {
        NativeResult execute(Value[] args, PrinterJob job) throws PrinterException;
    }

    private void printFunction(String name, PrintFunction function, Type returnType, Type... argTypes) {
        Type[] argTypesWithJob = new Type[argTypes.length + 1];
        System.arraycopy(argTypes, 0, argTypesWithJob, 1, argTypes.length);
        argTypesWithJob[0] = Types.INT;
        func(name, (args) -> {
            int id = args[0].asNumber().intValue();
            if (!jobs.containsKey(id)) {
                return NativeResult.Err("PrinterError", "Invalid job id");
            }
            PrinterJob job = jobs.get(id);
            Value[] argsWithoutJob = new Value[args.length - 1];
            System.arraycopy(args, 1, argsWithoutJob, 0, args.length - 1);
            try {
                return function.execute(argsWithoutJob, job);
            } catch (PrinterException e) {
                return NativeResult.Err("PrinterError", e.getMessage());
            }
        }, returnType, argTypesWithJob);
    }
}
