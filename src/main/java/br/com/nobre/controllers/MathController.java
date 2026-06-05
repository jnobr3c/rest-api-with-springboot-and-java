package br.com.nobre.controllers;

import br.com.nobre.math.SimpleMath;
import br.com.nobre.request.converters.NumberConverter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/math") // Define o path base para o controlador
public class MathController {

    private SimpleMath math = new SimpleMath();

    //http://localhost:8080/math/sum/3/5
    @RequestMapping(value="/sum/{numberOne}/{numberTwo}")
    public Double sum(@PathVariable("numberOne") String numberOne,
                      @PathVariable("numberTwo") String numberTwo
    ) throws Exception {
        if (!NumberConverter.isNumeric(numberOne) || !NumberConverter.isNumeric(numberTwo))
            throw new UnsupportedOperationException("Please set a numeric value!");

        return math.sum(NumberConverter.covertToDouble(numberOne), NumberConverter.covertToDouble(numberTwo));
    }

    //http://localhost:8080/math/subtraction/3/5
    @RequestMapping(value="/subtraction/{numberOne}/{numberTwo}")
    public Double subtraction(@PathVariable("numberOne") String numberOne,
                      @PathVariable("numberTwo") String numberTwo
    ) throws Exception {
        if (!NumberConverter.isNumeric(numberOne) || !NumberConverter.isNumeric(numberTwo))
            throw new UnsupportedOperationException("Please set a numeric value!");

        return math.subtraction(NumberConverter.covertToDouble(numberOne), NumberConverter.covertToDouble(numberTwo));
    }

    //http://localhost:8080/math/mutiplication/3/5
    @RequestMapping(value="/mutiplication/{numberOne}/{numberTwo}")
    public Double mutiplication(@PathVariable("numberOne") String numberOne,
                              @PathVariable("numberTwo") String numberTwo
    ) throws Exception {
        if (!NumberConverter.isNumeric(numberOne) || !NumberConverter.isNumeric(numberTwo))
            throw new UnsupportedOperationException("Please set a numeric value!");

        return math.mutiplication(NumberConverter.covertToDouble(numberOne), NumberConverter.covertToDouble(numberTwo));
    }

    //http://localhost:8080/math/division/3/5
    @RequestMapping(value="/division/{numberOne}/{numberTwo}")
    public Double division(@PathVariable("numberOne") String numberOne,
                                @PathVariable("numberTwo") String numberTwo
    ) throws Exception {
        if (!NumberConverter.isNumeric(numberOne) || !NumberConverter.isNumeric(numberTwo))
            throw new UnsupportedOperationException("Please set a numeric value!");

        return math.division(NumberConverter.covertToDouble(numberOne), NumberConverter.covertToDouble(numberTwo));
    }

    //http://localhost:8080/math/mean/3/5 => Média
    @RequestMapping(value="/mean/{numberOne}/{numberTwo}")
    public Double mean(@PathVariable("numberOne") String numberOne,
                           @PathVariable("numberTwo") String numberTwo
    ) throws Exception {
        if (!NumberConverter.isNumeric(numberOne) || !NumberConverter.isNumeric(numberTwo))
            throw new UnsupportedOperationException("Please set a numeric value!");

        return math.mean (NumberConverter.covertToDouble(numberOne), NumberConverter.covertToDouble(numberTwo)) ;
    }

    //http://localhost:8080/math/squareRoot/81 => Raiz Quadrada
    @RequestMapping(value="/squareRoot/{number}")
    public Double squareRoot(
            @PathVariable("number") String number
    ) throws Exception {
        if (!NumberConverter.isNumeric(number))
            throw new UnsupportedOperationException("Please set a numeric value!");
        //Double result = Math.sqrt(covertToDouble(number));
        //return result;
        return math.squareRoot(NumberConverter.covertToDouble(number));
    }

}


