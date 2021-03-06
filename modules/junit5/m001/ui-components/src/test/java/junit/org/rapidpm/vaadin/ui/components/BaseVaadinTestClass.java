package junit.org.rapidpm.vaadin.ui.components;

import com.vaadin.testbench.TestBench;
import com.vaadin.testbench.TestBenchTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rapidpm.ddi.DI;
import org.rapidpm.dependencies.core.net.PortUtils;
import org.rapidpm.frp.Transformations;
import org.rapidpm.frp.functions.CheckedExecutor;
import org.rapidpm.frp.functions.CheckedSupplier;
import org.rapidpm.microservice.Main;
import org.rapidpm.microservice.MainUndertow;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.function.Supplier;

import static org.rapidpm.frp.StringFunctions.notEmpty;
import static org.rapidpm.frp.StringFunctions.notStartsWith;
import static org.rapidpm.frp.Transformations.not;
import static org.rapidpm.microservice.MainUndertow.*;

/**
 *
 */
public abstract class BaseVaadinTestClass extends TestBenchTestCase {

  protected String url;
  Supplier<String> ipSupplierLocalIP = () -> {
    final CheckedSupplier<Enumeration<NetworkInterface>> checkedSupplier = NetworkInterface::getNetworkInterfaces;

    return Transformations.<NetworkInterface>enumToStream()
        .apply(checkedSupplier.getOrElse(Collections::emptyEnumeration))
        .map(NetworkInterface::getInetAddresses)
        .flatMap(iaEnum -> Transformations.<InetAddress>enumToStream().apply(iaEnum))
        .filter(inetAddress -> inetAddress instanceof Inet4Address)
        .filter(not(InetAddress::isMulticastAddress))
        .map(InetAddress::getHostAddress)
        .filter(notEmpty())
        .filter(adr -> notStartsWith().apply(adr, "127"))
        .filter(adr -> notStartsWith().apply(adr, "169.254"))
        .filter(adr -> notStartsWith().apply(adr, "255.255.255.255"))
        .filter(adr -> notStartsWith().apply(adr, "255.255.255.255"))
        .filter(adr -> notStartsWith().apply(adr, "0.0.0.0"))
        //            .filter(adr -> range(224, 240).noneMatch(nr -> adr.startsWith(valueOf(nr))))
        .findFirst().orElse("localhost");
  };

  @BeforeEach
  public void setUp()
      throws Exception {
    final PortUtils utils = new PortUtils();
    System.setProperty(REST_PORT_PROPERTY, utils.nextFreePortForTest() + "");
    System.setProperty(SERVLET_PORT_PROPERTY, utils.nextFreePortForTest() + "");
    System.setProperty(SERVLET_HOST_PROPERTY, ipSupplierLocalIP.get());
    System.setProperty(REST_HOST_PROPERTY, ipSupplierLocalIP.get());
    System.setProperty(MainUndertow.SHIRO_ACTIVE_PROPERTY, "false");
    url = "http://" + ipSupplierLocalIP.get() + ":" + System.getProperty(SERVLET_PORT_PROPERTY) + MainUndertow.MYAPP;

    DI.clearReflectionModel();
    DI.activatePackages("org.rapidpm");
    DI.activatePackages(this.getClass());
    DI.activateDI(this);
    Main.deploy();


//    final URL             hubURL          = new URL("http://" + ipSupplierLocalIP.get() + ":4444/wd/hub");
//    final RemoteWebDriver remoteWebDriver = new RemoteWebDriver(hubURL, DesiredCapabilities.chrome());
//    final WebDriver       webDriver       = TestBench.createDriver(remoteWebDriver);

//    setDriver(webDriver);
    System.setProperty("webdriver.chrome.driver", "./_data/webdrivers/chromedriver-mac-64bit");
    setDriver(new ChromeDriver());
//    setDriver(new FirefoxDriver());
//    setDriver(new SafariDriver());
//    setDriver(new OperaDriver());
//    getDriver().manage().window().maximize();
  }

  //TODO inheritance is slowing down without benefits
//  @Test
//  public void testAddressBook() {
//    getDriver().get(url);
//    Assert.assertTrue($(GridElement.class).exists());
//  }

  @AfterEach
  public void tearDown()
      throws Exception {
    ((CheckedExecutor) () -> getDriver().quit()).execute();
    ((CheckedExecutor) Main::stop).execute();
    ((CheckedExecutor) DI::clearReflectionModel).execute();

  }

}
